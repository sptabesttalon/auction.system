package vn.uet.auction.service;

import vn.uet.auction.model.*;
import vn.uet.auction.storage.*;
import java.io.*;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/** Authoritative single-writer transaction coordinator: clone -> validate -> persist -> publish. */
public final class AuctionService {
    private static AuctionService instance;
    public static synchronized AuctionService singleton(Repository repository, Clock clock) throws Exception {
        if (instance == null) instance = new AuctionService(repository, clock);
        return instance;
    }
    private final Repository repository;
    private final Clock clock;
    private State state;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final List<Consumer<String>> observers = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final SecureRandom random = new SecureRandom();
    private record Session(long userId, long expiresAt) { }
    public AuctionService(Repository repository, Clock clock) throws Exception {
        this.repository = repository; this.clock = clock;
        this.state = repository.load();
    }
    public void observe(Consumer<String> observer) { observers.add(observer); }
    private long now() { return clock.millis(); }
    public synchronized Map<String,Object> register(String username, String password, String role) {
        username = required(username, "Tên đăng nhập", 3, 32).toLowerCase(Locale.ROOT);
        if (!username.matches("[a-z0-9_.-]+")) throw bad("Tên đăng nhập chỉ gồm chữ thường, số, ., _, -");
        if (password == null || password.length() < 8 || password.length() > 128) throw bad("Mật khẩu cần 8–128 ký tự");
        String type = role == null ? "BIDDER" : role.toUpperCase(Locale.ROOT);
        if (!type.equals("BIDDER") && !type.equals("SELLER")) throw bad("Chỉ được đăng ký Bidder hoặc Seller");
        final String name = username;
        return transact(s -> {
            for (User u : s.users.values()) if (u.getUsername().equals(name)) throw bad("Tên đăng nhập đã tồn tại");
            String salt = PasswordUtil.newSalt(); String hash = PasswordUtil.hash(password,salt);
            User u = type.equals("SELLER") ? new Seller(s.nextUserId++,name,salt,hash)
                    : new Bidder(s.nextUserId++,name,salt,hash);
            s.users.put(u.getId(),u); return userJson(u);
        }, null);
    }
    /** Explicit local-only admin bootstrap; never provide a public admin-registration route. */
    public synchronized void bootstrapAdmin(String username, String password) {
        if (username == null || password == null || password.length() < 8) return;
        for (User u : state.users.values()) if (u instanceof Admin) return;
        transact(s -> {
            String salt = PasswordUtil.newSalt();
            Admin admin = new Admin(s.nextUserId++, username.toLowerCase(Locale.ROOT),salt,PasswordUtil.hash(password,salt));
            s.users.put(admin.getId(),admin);
            return null;
        }, null);
    }
    public synchronized Map<String,Object> login(String username, String password) {
        User user = state.users.values().stream().filter(u -> u.getUsername().equalsIgnoreCase(username == null ? "" : username.trim())).findFirst().orElse(null);
        if (user == null || !PasswordUtil.verify(password == null ? "" : password,user.getSalt(),user.getPasswordHash()))
            throw new AuthenticationException("Sai tài khoản hoặc mật khẩu");
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new Session(user.getId(),now()+ 24L*3600_000));
        return Map.of("token",token,"user",userJson(user));
    }
    public void logout(String token) { sessions.remove(token); }
    public synchronized Map<String,Object> me(String token) { return userJson(auth(token)); }
    private User auth(String token) {
        Session session = sessions.get(token);
        if (session == null || session.expiresAt < now()) throw new AuthenticationException("Vui lòng đăng nhập");
        User user = state.users.get(session.userId);
        if (user == null) throw new AuthenticationException("Phiên không hợp lệ");
        return user;
    }
    private static Map<String,Object> userJson(User u) {
        return Map.of("id",u.getId(),"username",u.getUsername(),"role",u.role());
    }
    public synchronized List<Map<String,Object>> list() {
        return state.auctions.values().stream().sorted(Comparator.comparingLong((Auction a) -> a.getId()).reversed())
            .map(a -> auctionJson(a,false)).toList();
    }
    public synchronized Map<String,Object> detail(long id) { return auctionJson(auction(state,id),true); }
    public synchronized Map<String,Object> create(String token, String category, String title, String description,
                                                 long startPrice, long startTime, long endTime) {
        User u = auth(token); requireSeller(u);
        title = required(title,"Tên sản phẩm",3,120); description = required(description,"Mô tả",5,2000);
        if (startPrice < 1000 || startPrice > 10_000_000_000_000L) throw bad("Giá khởi điểm từ 1.000 đến 10 nghìn tỷ VND");
        if (startTime < now() - 60_000 || endTime <= Math.max(now(), startTime) || endTime - startTime > 365L*24*3600_000)
            throw bad("Thời gian bắt đầu/kết thúc không hợp lệ");
        String finalTitle = title, finalDescription = description;
        Map<String,Object> result = transact(s -> {
            long id = s.nextAuctionId++;
            Item item;
            try { item= ItemFactory.create(id,category,finalTitle,finalDescription); }
            catch (IllegalArgumentException e) { throw bad(e.getMessage()); }
            Auction a = new Auction(id,u.getId(),item,startPrice,startTime,endTime);
            a.status = startTime <= now() ? Auction.Status.RUNNING : Auction.Status.OPEN;
            s.auctions.put(id,a); return auctionJson(a,true,s);
        }, "auction_created");
        return result;
    }
    public synchronized Map<String,Object> edit(String token, long id, String title, String description) {
        User u = auth(token);
        title = required(title,"Tên",3,120); description=required(description,"Mô tả",5,2000);
        String t=title,d=description;
        return transact(s -> {
            Auction a=auction(s,id); ownerOrAdmin(u,a);
            if (a.status != Auction.Status.OPEN && a.status != Auction.Status.RUNNING || !a.history.isEmpty()) throw bad("Chỉ được sửa trước khi có bid");
            a.item.edit(t,d); a.version++; return auctionJson(a,true,s);
        },"auction_updated");
    }
    public synchronized Map<String,Object> delete(String token,long id) {
        User u = auth(token);
        return transact(s -> {
            Auction a=auction(s,id); ownerOrAdmin(u,a);
            if (!a.history.isEmpty() || a.status == Auction.Status.FINISHED || a.status == Auction.Status.PAID) throw bad("Không thể xóa phiên đã có bid / đã kết thúc");
            s.auctions.remove(id); return Map.of("id",id,"deleted",true);
        },"auction_deleted");
    }
    public synchronized Map<String,Object> bid(String token,long id,long amount) {
        User user=auth(token);
        if (!(user instanceof Bidder)) throw new ServiceException(403,"Chỉ Bidder được tham gia đấu giá");
        return transact(s -> {
            Auction a=auction(s,id); checkRunning(a,user,now());
            if (amount <= a.currentPrice) throw new InvalidBidException("Giá đặt phải lớn hơn giá hiện tại");
            if (amount > 10_000_000_000_000L) throw new InvalidBidException("Vượt hạn mức giá");
            addBid(s,a,user.getId(),amount,false);
            resolveAuto(s,a);
            return auctionJson(a,true,s);
        },"bid_placed");
    }
    public synchronized Map<String,Object> setAuto(String token,long id,long maxAmount,long increment) {
        User user=auth(token);
        if (!(user instanceof Bidder)) throw new ServiceException(403,"Chỉ Bidder được đăng ký auto-bid");
        return transact(s -> {
            Auction a=auction(s,id); checkRunning(a,user,now());
            if (increment < 1000 || increment > 1_000_000_000L || maxAmount <= a.currentPrice || maxAmount > 10_000_000_000_000L)
                throw new InvalidBidException("Giới hạn tự động phải cao hơn giá hiện tại, bước giá tối thiểu 1.000 VND");
            a.autoBids.removeIf(rule -> rule.bidderId == user.getId());
            a.autoBids.add(new AutoBid(user.getId(), maxAmount, increment, now(), s.nextAutoBidOrder++));
            resolveAuto(s,a); return auctionJson(a,true,s);
        },"bid_placed");
    }
    public synchronized Map<String,Object> cancel(String token,long id) {
        User u=auth(token);
        return transact(s -> {
            Auction a=auction(s,id); ownerOrAdmin(u,a);
            if (a.status == Auction.Status.PAID || a.status == Auction.Status.CANCELED) throw bad("Phiên đã thanh toán / đã hủy không thể hủy tiếp");
            if (!a.history.isEmpty() && !(u instanceof Admin)) throw new ServiceException(403,"Chỉ Admin được hủy phiên đã có bid");
            a.status= Auction.Status.CANCELED; a.leaderId=null; a.autoBids.clear(); a.version++;
            return auctionJson(a,true,s);
        },"auction_updated");
    }
    public synchronized Map<String,Object> pay(String token,long id) {
        User u=auth(token);
        return transact(s -> {
            Auction a=auction(s,id); ownerOrAdmin(u,a);
            if (a.status != Auction.Status.FINISHED || a.leaderId == null) throw bad("Chỉ xác nhận thanh toán khi có người thắng và phiên đã kết thúc");
            a.status= Auction.Status.PAID; a.version++; return auctionJson(a,true,s);
        },"auction_updated");
    }
    /** Scheduler transitions OPEN->RUNNING->FINISHED on server time. */
    public synchronized void tick() {
        boolean changed=false;
        for (Auction a : state.auctions.values()) {
            if (a.status == Auction.Status.OPEN && now()>=a.startTime) {changed=true;break;}
            if (a.status == Auction.Status.RUNNING && now()>=a.endTime) {changed=true;break;}
        }
        if (changed) transact(s -> {
            for (Auction a : s.auctions.values()) {
                if (a.status == Auction.Status.OPEN && now()>=a.startTime) {a.status= Auction.Status.RUNNING;a.version++;}
                if (a.status == Auction.Status.RUNNING && now()>=a.endTime) {a.status= Auction.Status.FINISHED;a.version++;}
            }
            return Map.of("updated",true);
        },"auction_updated");
    }
    private void checkRunning(Auction a,User bidder,long now) {
        if (a.status != Auction.Status.RUNNING || now < a.startTime || now >= a.endTime) throw new AuctionClosedException();
        if (a.sellerId == bidder.getId()) throw new ServiceException(403,"Không được đấu giá sản phẩm của chính mình");
    }
    private void addBid(State s,Auction a,long uid,long amount,boolean automatic) {
        if (amount <= a.currentPrice) throw new InvalidBidException("Giá đấu không thể giảm");
        if (now() >= a.endTime) throw new AuctionClosedException();
        a.currentPrice = amount; a.leaderId=uid; a.version++;
        a.history.add(new BidTransaction(s.nextBidId++, uid,amount,now(),automatic));
        // Anti-sniping: when remaining time <= 30s, extend until >= now + 60s.
        if (a.endTime-now() <= 30_000) a.endTime=Math.max(a.endTime,now()+60_000);
    }
    private void resolveAuto(State s,Auction a) {
        // Proxy rules sorted by max, then registration time. Every automatic offer is
        // a real committed bid. A safety limit prevents pathological billion-step loops.
        Comparator<AutoBid> ranking = Comparator.comparingLong((AutoBid rule) -> rule.maxAmount).reversed()
            .thenComparingLong(rule -> rule.registeredAt)
            .thenComparingLong(rule -> rule.registrationOrder);
        for (int iteration=0;iteration<500;iteration++) {
            PriorityQueue<AutoBid> eligible = new PriorityQueue<>(ranking);
            for (AutoBid rule : a.autoBids) {
                if (a.leaderId != null && rule.bidderId == a.leaderId) continue;
                if (a.currentPrice > Long.MAX_VALUE - rule.increment) continue;
                if (rule.maxAmount >= a.currentPrice + rule.increment) {
                    // When equal maximums conflict, the earlier registered auto-rule
                    // has priority over later registration reaching that same ceiling.
                    boolean losesTie = a.autoBids.stream().anyMatch(other ->
                        other.bidderId != rule.bidderId && other.maxAmount == rule.maxAmount &&
                        (other.registeredAt < rule.registeredAt ||
                         (other.registeredAt == rule.registeredAt && other.registrationOrder < rule.registrationOrder)) &&
                        a.currentPrice + rule.increment == rule.maxAmount);
                    if (!losesTie) eligible.add(rule);
                }
            }
            AutoBid chosen = eligible.poll();
            if (chosen == null) return;
            addBid(s, a, chosen.bidderId, a.currentPrice + chosen.increment, true);
        }
        throw new InvalidBidException("Các mức auto-bid tạo quá nhiều bước; hãy tăng bước giá");
    }
    private static ServiceException bad(String reason) { return new ServiceException(400,reason); }
    private static String required(String value,String field,int min,int max) {
        if (value == null || value.trim().length()<min || value.trim().length()>max) throw bad(field+" phải từ "+min+" đến "+max+" ký tự");
        return value.trim();
    }
    private static void requireSeller(User u) { if (!(u instanceof Seller) && !(u instanceof Admin)) throw new ServiceException(403,"Cần tài khoản Seller"); }
    private static void ownerOrAdmin(User u,Auction a) { if (u.getId()!=a.sellerId && !(u instanceof Admin)) throw new ServiceException(403,"Bạn không sở hữu sản phẩm này"); }
    private static Auction auction(State s,long id) {
        Auction a=s.auctions.get(id);
        if (a == null) throw new ServiceException(404,"Không tìm thấy phiên đấu giá");
        return a;
    }
    private Map<String,Object> auctionJson(Auction a,boolean detailed) { return auctionJson(a,detailed,state); }
    private static Map<String,Object> auctionJson(Auction a,boolean detailed,State source) {
        Map<String,Object> r=new LinkedHashMap<>();
        r.put("id",a.getId()); r.put("title",a.item.title()); r.put("description",a.item.description());
        r.put("category",a.item.category()); r.put("icon",a.item.icon());
        r.put("startPrice",a.startPrice); r.put("currentPrice",a.currentPrice);
        r.put("startTime",a.startTime); r.put("endTime",a.endTime);
        r.put("status",a.status.name()); r.put("version",a.version);
        r.put("sellerId",a.sellerId); r.put("seller",source.users.get(a.sellerId).getUsername());
        r.put("leaderId",a.leaderId);
        r.put("leader",a.leaderId==null?null:source.users.get(a.leaderId).getUsername());
        r.put("bidCount",a.history.size());
        if (detailed) {
            List<Map<String,Object>> h=new ArrayList<>();
            for (BidTransaction b:a.history) h.add(Map.of("id",b.getId(),"bidder",source.users.get(b.bidderId).getUsername(),
                "bidderId",b.bidderId,"amount",b.amount,"createdAt",b.createdAt,"automatic",b.automatic));
            r.put("history",h);
            r.put("autoBidCount",a.autoBids.size());
        }
        return r;
    }
    @FunctionalInterface private interface Tx<T> { T apply(State state); }
    private <T> T transact(Tx<T> action,String event) {
        try {
            State copy=deepCopy(state);
            T result=action.apply(copy);
            repository.save(copy); // Atomic persistence first; no visible in-memory mutation before success.
            state=copy;
            if (event!=null) {
                String message="{\"type\":\""+event+"\",\"time\":"+now()+"}";
                for (Consumer<String> observer:observers) {
                    try {observer.accept(message);} catch (RuntimeException ignored) { }
                }
            }
            return result;
        } catch (ServiceException e) { throw e; }
          catch (Exception e) { throw new ServiceException(500,"Lỗi lưu dữ liệu; giao dịch đã được hủy"); }
    }
    private static State deepCopy(State original) throws IOException,ClassNotFoundException {
        ByteArrayOutputStream buffer=new ByteArrayOutputStream();
        try (ObjectOutputStream out=new ObjectOutputStream(buffer)) { out.writeObject(original); }
        try (ObjectInputStream in=new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {return (State)in.readObject();}
    }
}
