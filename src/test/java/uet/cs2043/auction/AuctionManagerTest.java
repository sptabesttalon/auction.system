package uet.cs2043.auction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uet.cs2043.auction.common.exceptions.AuctionClosedException;
import uet.cs2043.auction.common.exceptions.InvalidBidException;
import uet.cs2043.auction.common.models.Auction;
import uet.cs2043.auction.server.controller.AuctionManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionManagerTest {
    private AuctionManager manager;

    @BeforeEach
    public void setUp() {
        manager = AuctionManager.getInstance();
    }

    @Test
    public void testInvalidBidLowerThanCurrentPrice() {
        Auction auction = manager.createAuction("iPhone 16", "New", "seller1", 1000, 100, 60);
        assertThrows(InvalidBidException.class, () -> {
            manager.placeBid(auction.getId(), "bidder1", 900); // Nhỏ hơn 1000 -> Phải ném lỗi
        });
    }

    @Test
    public void testAntiSnipingExtension() throws Exception {
        // Tạo phiên chỉ còn sống 10 giây (nằm trong ngưỡng <= 30 giây cuối)
        Auction auction = manager.createAuction("Tranh Sơn Dầu", "Art", "seller1", 1000, 100, 10);
        long originalEndTime = auction.getEndTime();

        manager.placeBid(auction.getId(), "bidder1", 1200);

        // Kiểm tra thời gian kết thúc đã được cộng thêm 60.000ms (60 giây)
        assertEquals(originalEndTime + 60_000L, auction.getEndTime());
    }

    @Test
    public void testAutoBiddingCompetition() throws Exception {
        Auction auction = manager.createAuction("Xe Máy SH", "2025", "seller1", 1000, 100, 120);

        // Quang Anh đặt Auto-Bid tối đa 2000, bước giá 100
        manager.registerAutoBid(auction.getId(), "QuangAnh", 2000, 100);
        // Văn Anh đặt Auto-Bid tối đa 2500, bước giá 100
        manager.registerAutoBid(auction.getId(), "VanAnh", 2500, 100);

        // Hệ thống phải tự động cho Văn Anh thắng ở mức 2000 + 100 = 2100
        assertEquals("VanAnh", auction.getCurrentWinner());
        assertEquals(2100.0, auction.getCurrentHighestBid());
    }

    @Test
    public void testConcurrentBiddingNoRaceCondition() throws InterruptedException {
        Auction auction = manager.createAuction("Laptop Gaming", "RTX 4090", "seller1", 1000, 100, 120);
        int numThreads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);

        // 20 luồng cùng đặt đúng 1 mức giá 1200 tại cùng 1 mili-giây
        for (int i = 0; i < numThreads; i++) {
            final String user = "Bidder_" + i;
            pool.submit(() -> {
                try {
                    latch.await(); // Đợi hiệu lệnh xuất phát cùng lúc
                    manager.placeBid(auction.getId(), user, 1200);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.countDown(); // Cho phép cả 20 luồng lao vào cùng lúc
        doneLatch.await();
        pool.shutdown();

        // Nhờ ReentrantLock, CHỈ DUY NHẤT 1 người đặt mức 1200 thành công, 19 người còn lại bị từ chối vì giá lúc đó đã đòi >= 1300
        assertEquals(1, successCount.get());
        assertEquals(1200.0, auction.getCurrentHighestBid());
    }
}