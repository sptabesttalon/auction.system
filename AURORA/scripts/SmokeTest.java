import vn.uet.auction.service.*;
import vn.uet.auction.storage.*;
import vn.uet.auction.web.Json;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;

public final class SmokeTest {
    static final class Mem implements Repository {
        State s=new State();
        public State load(){return s;}
        public void save(State s){this.s=s;}
    }
    private static void ok(boolean b,String msg){if(!b)throw new AssertionError(msg);}
    public static void main(String[] args)throws Exception {
        Mem repo=new Mem();AuctionService svc=new AuctionService(repo,Clock.systemUTC());
        svc.register("seller_smoke","password123","SELLER");
        svc.register("bidder_one","password123","BIDDER");
        svc.register("bidder_two","password123","BIDDER");
        String seller=(String)svc.login("seller_smoke","password123").get("token");
        String u=(String)svc.login("bidder_one","password123").get("token");
        String v=(String)svc.login("bidder_two","password123").get("token");
        long now=System.currentTimeMillis();long id=(long)svc.create(seller,"ART","Test item","Test item description",1000,now,now+300000).get("id");
        try {svc.bid(seller,id,2000);throw new AssertionError("Seller was allowed to bid");}catch(ServiceException expected){}
        svc.bid(u,id,2000);
        try{svc.bid(v,id,1500);throw new AssertionError("Rollback accepted");}catch(InvalidBidException expected){}
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch gate=new CountDownLatch(1);
        List<Future<?>> jobs=new ArrayList<>();for(String who:List.of(u,v))jobs.add(pool.submit(()->{
            gate.await();try{svc.bid(who,id,3000);}catch(InvalidBidException expected){}return null;
        }));
        gate.countDown();for(Future<?> future:jobs)future.get();pool.shutdownNow();
        Map<String,Object> current=svc.detail(id);
        ok((long)current.get("currentPrice")==3000,"Final price must be exactly 3000");
        ok((int)current.get("bidCount")==2,"Concurrent equal bids must have one winner");
        svc.setAuto(u,id,10000,1000);svc.bid(v,id,5000);
        ok((long)svc.detail(id).get("currentPrice")==6000,"Auto bid must respond");
        ok(((Map<?,?>)Json.parse("{\"x\":123}")).get("x").equals(123L),"JSON longs parse correctly");
        System.out.println("PASSED: registration, authentication, permissions, bidding, no rollback, concurrent bid, auto-bid, JSON parsing");
    }
}
