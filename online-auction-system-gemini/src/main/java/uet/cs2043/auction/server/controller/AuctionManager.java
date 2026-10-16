package uet.cs2043.auction.server.controller;

import uet.cs2043.auction.common.exceptions.AuctionClosedException;
import uet.cs2043.auction.common.exceptions.InvalidBidException;
import uet.cs2043.auction.common.models.Auction;
import uet.cs2043.auction.common.models.AutoBidConfig;
import uet.cs2043.auction.common.models.BidTransaction;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionManager {
    // 1. SINGLETON PATTERN
    private static AuctionManager instance;

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    // 2. OBSERVER INTERFACE CHO REALTIME UPDATE
    public interface AuctionObserver {
        void onAuctionUpdated(Auction auction);
        void onAuctionClosed(Auction auction);
    }

    private final Map<String, Auction> auctions = new ConcurrentHashMap<>();
    private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Cấu hình Anti-sniping: Nếu bid trong 30 giây cuối -> cộng thêm 60 giây
    private static final long ANTI_SNIPING_WINDOW_MS = 30_000L;
    private static final long ANTI_SNIPING_EXTENSION_MS = 60_000L;

    private AuctionManager() {
        // Luồng ngầm kiểm tra mỗi giây để tự động đóng các phiên hết hạn
        scheduler.scheduleAtFixedRate(this::checkAndCloseExpiredAuctions, 1, 1, TimeUnit.SECONDS);
    }

    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    private void notifyAuctionUpdated(Auction auction) {
        for (AuctionObserver obs : observers) {
            obs.onAuctionUpdated(auction);
        }
    }

    private void notifyAuctionClosed(Auction auction) {
        for (AuctionObserver obs : observers) {
            obs.onAuctionClosed(auction);
        }
    }

    public Auction createAuction(String name, String desc, String seller,
                                 double startPrice, double minStep, long durationSec) {
        Auction auction = new Auction(name, desc, seller, startPrice, minStep, durationSec);
        auctions.put(auction.getId(), auction);
        notifyAuctionUpdated(auction);
        return auction;
    }

    public Collection<Auction> getAllAuctions() {
        return auctions.values();
    }

    public Auction getAuction(String auctionId) {
        return auctions.get(auctionId);
    }

    // 3. XỬ LÝ ĐẤU GIÁ ĐỒNG THỜI + ANTI-SNIPING + AUTO-BIDDING
    public void placeBid(String auctionId, String bidderUsername, double amount)
            throws AuctionClosedException, InvalidBidException {
        Auction auction = auctions.get(auctionId);
        if (auction == null) {
            throw new InvalidBidException("Không tìm thấy phiên đấu giá!");
        }

        ReentrantLock lock = auction.getLock();
        lock.lock(); // BẮT ĐẦU VÙNG CRITICAL SECTION (An toàn tuyệt đối trước đa luồng)
        try {
            long now = System.currentTimeMillis();

            // Kiểm tra phiên đã đóng chưa
            if (auction.getStatus() != Auction.Status.RUNNING || now >= auction.getEndTime()) {
                auction.setStatus(Auction.Status.FINISHED);
                throw new AuctionClosedException("Phiên đấu giá đã kết thúc!");
            }

            // Kiểm tra bước giá hợp lệ (Nếu chưa có ai đặt thì phải >= giá khởi điểm, nếu có rồi phải >= giá hiện tại + bước giá)
            double requiredMin = auction.getBidHistory().isEmpty()
                    ? auction.getStartingPrice()
                    : auction.getCurrentHighestBid() + auction.getMinIncrement();

            if (amount < requiredMin) {
                throw new InvalidBidException(
                        String.format("Giá đặt (%.0f) thấp hơn mức tối thiểu hợp lệ (%.0f)!", amount, requiredMin));
            }

            // Ghi nhận lượt đặt giá hợp lệ
            recordValidBid(auction, bidderUsername, amount, now);

            // Kích hoạt thuật toán Auto-Bidding ngay trong vùng Lock để tránh xung đột
            processAutoBidding(auction);

        } finally {
            lock.unlock(); // GIẢI PHÓNG KHÓA DÙ CÓ NGOẠI LỆ HAY KHÔNG
        }

        // Thông báo Realtime tới tất cả Client sau khi đã nhả khóa
        notifyAuctionUpdated(auction);
    }

    // 4. ĐĂNG KÝ AUTO-BIDDING (ĐẤU GIÁ TỰ ĐỘNG)
    public void registerAutoBid(String auctionId, String bidderUsername, double maxBid, double increment)
            throws AuctionClosedException, InvalidBidException {
        Auction auction = auctions.get(auctionId);
        if (auction == null) {
            throw new InvalidBidException("Phiên đấu giá không tồn tại!");
        }

        ReentrantLock lock = auction.getLock();
        lock.lock();
        try {
            if (auction.getStatus() != Auction.Status.RUNNING || System.currentTimeMillis() >= auction.getEndTime()) {
                throw new AuctionClosedException("Phiên đấu giá đã đóng, không thể cài Auto-Bid!");
            }
            if (increment < auction.getMinIncrement()) {
                throw new InvalidBidException("Bước giá tự động không được nhỏ hơn bước giá tối thiểu của phiên!");
            }
            if (maxBid <= auction.getCurrentHighestBid()) {
                throw new InvalidBidException("Giá tối đa (maxBid) phải lớn hơn giá hiện tại!");
            }

            // Xóa cấu hình cũ của cùng user (nếu có) và thêm cấu hình mới vào PriorityQueue
            auction.getAutoBids().removeIf(cfg -> cfg.getBidderUsername().equals(bidderUsername));
            auction.getAutoBids().add(new AutoBidConfig(bidderUsername, maxBid, increment));

            // Kích hoạt so khớp Auto-Bid ngay lập tức
            processAutoBidding(auction);
        } finally {
            lock.unlock();
        }
        notifyAuctionUpdated(auction);
    }

    // Logic cốt lõi của Auto-Bidding dùng PriorityQueue
    private void processAutoBidding(Auction auction) {
        PriorityQueue<AutoBidConfig> queue = auction.getAutoBids();
        // Lọc bỏ các Auto-Bid đã bị vượt quá maxBid
        queue.removeIf(cfg -> cfg.getMaxBid() <= auction.getCurrentHighestBid());

        if (queue.isEmpty()) {
            return;
        }

        // TH1: Chỉ có 1 Auto-Bid đủ điều kiện trong hàng đợi
        if (queue.size() == 1) {
            AutoBidConfig topAuto = queue.peek();
            // Nếu người này chưa phải là người dẫn đầu -> Tự động trả giá vượt lên
            if (!topAuto.getBidderUsername().equals(auction.getCurrentWinner())) {
                double nextBid = Math.min(topAuto.getMaxBid(),
                        auction.getCurrentHighestBid() + topAuto.getIncrement());
                if (nextBid > auction.getCurrentHighestBid()) {
                    recordValidBid(auction, topAuto.getBidderUsername(), nextBid, System.currentTimeMillis());
                }
            }
            return;
        }

        // TH2: Có từ 2 Auto-Bid trở lên cạnh tranh trực tiếp với nhau
        AutoBidConfig first = queue.poll();  // Người có maxBid cao nhất (hoặc đăng ký sớm hơn nếu bằng giá)
        AutoBidConfig second = queue.peek(); // Người có maxBid cao thứ hai

        // Nếu 2 người có cùng mức maxBid, người đăng ký trước (first) sẽ thắng ở đúng mức maxBid đó
        double winningPrice;
        if (Double.compare(first.getMaxBid(), second.getMaxBid()) == 0) {
            winningPrice = first.getMaxBid();
        } else {
            // Người thứ nhất vượt qua mức tối đa của người thứ hai 1 bước giá (nhưng không vượt quá maxBid của mình)
            winningPrice = Math.min(first.getMaxBid(), second.getMaxBid() + first.getIncrement());
        }

        if (winningPrice > auction.getCurrentHighestBid()
                && !first.getBidderUsername().equals(auction.getCurrentWinner())) {
            recordValidBid(auction, first.getBidderUsername(), winningPrice, System.currentTimeMillis());
        }

        // Đưa lại người dẫn đầu vào hàng đợi để bảo vệ vị trí trước các lượt bid thủ công sau này
        queue.add(first);
        queue.removeIf(cfg -> cfg.getMaxBid() <= auction.getCurrentHighestBid());
    }

    // Ghi nhận Bid và kiểm tra Anti-Sniping
    private void recordValidBid(Auction auction, String bidderUsername, double amount, long now) {
        auction.setCurrentHighestBid(amount);
        auction.setCurrentWinner(bidderUsername);
        auction.getBidHistory().add(new BidTransaction(auction.getId(), bidderUsername, amount));

        // 5. THUẬT TOÁN ANTI-SNIPING: Nếu còn <= 30 giây thì cộng thêm 60 giây
        long remainingTime = auction.getEndTime() - now;
        if (remainingTime > 0 && remainingTime <= ANTI_SNIPING_WINDOW_MS) {
            auction.setEndTime(auction.getEndTime() + ANTI_SNIPING_EXTENSION_MS);
        }
    }

    // 6. TỰ ĐỘNG ĐÓNG PHIÊN KHI HẾT GIỜ
    private void checkAndCloseExpiredAuctions() {
        long now = System.currentTimeMillis();
        for (Auction auction : auctions.values()) {
            ReentrantLock lock = auction.getLock();
            if (lock.tryLock()) {
                try {
                    if (auction.getStatus() == Auction.Status.RUNNING && now >= auction.getEndTime()) {
                        auction.setStatus(Auction.Status.FINISHED);
                        notifyAuctionClosed(auction);
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
    }
}