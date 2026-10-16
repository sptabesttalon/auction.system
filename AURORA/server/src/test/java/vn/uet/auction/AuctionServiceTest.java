package vn.uet.auction;

import org.junit.jupiter.api.*;
import vn.uet.auction.service.*;
import vn.uet.auction.storage.*;
import vn.uet.auction.web.Json;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

final class AuctionServiceTest {
    static final class MemoryRepository implements Repository {
        State copy=new State();boolean fail=false;
        @Override public State load(){return copy;}
        @Override public void save(State value)throws Exception{
            if(fail)throw new IllegalStateException("Disk failure");
            // Save the same reference; service never mutates committed State after publish.
            copy=value;
        }
    }
    static final class MutableClock extends Clock {
        long value=1_800_000_000_000L;
        @Override public ZoneId getZone(){return ZoneOffset.UTC;}
        @Override public Clock withZone(ZoneId zone){return this;}
        @Override public Instant instant(){return Instant.ofEpochMilli(value);}
        @Override public long millis(){return value;}
    }
    MemoryRepository repo;
    MutableClock clock;
    AuctionService service;
    String seller,alice,bob;
    long auctionId;
    @BeforeEach void setUp()throws Exception{
        repo=new MemoryRepository();clock=new MutableClock();service=new AuctionService(repo,clock);
        service.register("selleruser","password123","SELLER");
        service.register("alice_01","password123","BIDDER");
        service.register("bob_02","password123","BIDDER");
        seller=(String)service.login("selleruser","password123").get("token");
        alice=(String)service.login("alice_01","password123").get("token");
        bob=(String)service.login("bob_02","password123").get("token");
        auctionId=((Number)service.create(seller,"ELECTRONICS","Laptop demo","Mô tả đủ dài",1_000,clock.value,clock.value+120_000).get("id")).longValue();
    }
    private long price(){return ((Number)service.detail(auctionId).get("currentPrice")).longValue();}
    @Test void validBidUpdatesHighestAndHistory(){
        Map<String,Object> result=service.bid(alice,auctionId,2_000);
        assertEquals(2_000L,result.get("currentPrice"));
        assertEquals("alice_01",result.get("leader"));
        assertEquals(1,((List<?>)result.get("history")).size());
    }
    @Test void invalidBidNeverLowersPrice(){
        service.bid(alice,auctionId,5_000);
        assertThrows(InvalidBidException.class,()->service.bid(bob,auctionId,4_000));
        assertThrows(InvalidBidException.class,()->service.bid(bob,auctionId,5_000));
        assertEquals(5_000L,price());
    }
    @Test void rejectUnauthorizedRoles(){
        assertThrows(ServiceException.class,()->service.bid(seller,auctionId,2_000));
        assertThrows(ServiceException.class,()->service.create(alice,"ART","My art","Valid description",1_000,clock.value,clock.value+120_000));
        assertThrows(AuthenticationException.class,()->service.me("wrong-token"));
        assertThrows(AuthenticationException.class,()->service.login("alice_01","wrong-password"));
    }
    @Test void concurrentEqualBidsOnlyOneIsCommitted()throws Exception{
        CountDownLatch ready=new CountDownLatch(2),start=new CountDownLatch(1);
        ExecutorService pool=Executors.newFixedThreadPool(2);AtomicInteger ok=new AtomicInteger();
        List<Future<?>> futures=new ArrayList<>();
        for(String bidder:List.of(alice,bob))futures.add(pool.submit(()->{
            ready.countDown();start.await();try{service.bid(bidder,auctionId,2_000);ok.incrementAndGet();}
            catch(InvalidBidException ignored){}return null;
        }));
        assertTrue(ready.await(3,TimeUnit.SECONDS));start.countDown();
        for(Future<?> f:futures)f.get(5,TimeUnit.SECONDS);pool.shutdownNow();
        assertEquals(1,ok.get());assertEquals(2_000,price());
        assertEquals(1,((List<?>)service.detail(auctionId).get("history")).size());
    }
    @Test void concurrencyNeverRollsBackPrice()throws Exception{
        int count=30;ExecutorService pool=Executors.newFixedThreadPool(8);
        CountDownLatch start=new CountDownLatch(1);List<Future<?>> futures=new ArrayList<>();
        for(int i=0;i<count;i++){
            long amount=2_000+i*1_000;String bidder=i%2==0?alice:bob;
            futures.add(pool.submit(()->{start.await();try{service.bid(bidder,auctionId,amount);}catch(InvalidBidException ignored){}return null;}));
        }
        start.countDown();for(Future<?> f:futures)f.get(8,TimeUnit.SECONDS);pool.shutdownNow();
        assertEquals(31_000L,price());
        @SuppressWarnings("unchecked") List<Map<String,Object>> history=(List<Map<String,Object>>)service.detail(auctionId).get("history");
        long previous=1_000;
        for(Map<String,Object> bid:history){assertTrue((long)bid.get("amount")>previous);previous=(long)bid.get("amount");}
    }
    @Test void autoBiddingRespectsMaximum(){
        service.setAuto(alice,auctionId,8_000,1_000);
        service.bid(bob,auctionId,3_000);
        assertEquals(4_000,price());
        assertEquals("alice_01",service.detail(auctionId).get("leader"));
        service.bid(bob,auctionId,9_000);
        assertEquals(9_000,price());
        assertEquals("bob_02",service.detail(auctionId).get("leader"));
    }
    @Test void antiSnipingExtendsClock(){
        clock.value+=111_000;service.bid(alice,auctionId,2_000);
        Map<String,Object> result=service.detail(auctionId);
        assertEquals(clock.value+60_000,result.get("endTime"));
        clock.value+=10_000;service.tick();
        assertEquals("RUNNING",service.detail(auctionId).get("status"));
    }
    @Test void schedulerClosesAuctionAndPaidNeedsWinner(){
        service.bid(alice,auctionId,2_000);clock.value+=120_001;service.tick();
        assertEquals("FINISHED",service.detail(auctionId).get("status"));
        assertThrows(AuctionClosedException.class,()->service.bid(bob,auctionId,3_000));
        service.pay(seller,auctionId);
        assertEquals("PAID",service.detail(auctionId).get("status"));
    }
    @Test void failedPersistenceDoesNotPublishOrChangeState(){
        repo.fail=true;assertThrows(ServiceException.class,()->service.bid(alice,auctionId,2_000));
        assertEquals(1_000,price());
        assertEquals(0,service.detail(auctionId).get("bidCount"));
    }
    @Test void itemFactoryAndJson(){
        assertEquals("ART",vn.uet.auction.model.ItemFactory.create(9,"ART","Tranh","Tranh đẹp").category());
        assertEquals(42L,((Map<?,?>)Json.parse("{\"x\":42,\"y\":[true,null]}" )).get("x"));
    }
}
