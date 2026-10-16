package uet.cs2043.auction.common.models;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;

public class Auction extends Entity {
    private static final long serialVersionUID = 1L;

    public enum Status { OPEN, RUNNING, FINISHED, PAID, CANCELED }

    private String itemName;
    private String itemDescription;
    private String sellerUsername;
    private double startingPrice;
    private double currentHighestBid;
    private double minIncrement;
    private String currentWinner;
    private long startTime;
    private long endTime;
    private Status status;

    private final List<BidTransaction> bidHistory = new ArrayList<>();
    private final PriorityQueue<AutoBidConfig> autoBids = new PriorityQueue<>();

    // Transient vì Lock không tuần tự hóa qua mạng, chỉ dùng ở Server để đồng bộ đa luồng
    private transient ReentrantLock lock = new ReentrantLock(true); // Fair lock = true

    public Auction(String itemName, String itemDescription, String sellerUsername,
                   double startingPrice, double minIncrement, long durationSeconds) {
        super();
        this.itemName = itemName;
        this.itemDescription = itemDescription;
        this.sellerUsername = sellerUsername;
        this.startingPrice = startingPrice;
        this.currentHighestBid = startingPrice;
        this.minIncrement = minIncrement;
        this.currentWinner = "Chưa có";
        this.startTime = System.currentTimeMillis();
        this.endTime = this.startTime + (durationSeconds * 1000);
        this.status = Status.RUNNING;
    }

    public synchronized ReentrantLock getLock() {
        if (lock == null) {
            lock = new ReentrantLock(true);
        }
        return lock;
    }

    // Getters & Setters
    public String getItemName() { return itemName; }
    public String getItemDescription() { return itemDescription; }
    public String getSellerUsername() { return sellerUsername; }
    public double getStartingPrice() { return startingPrice; }
    public double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(double currentHighestBid) { this.currentHighestBid = currentHighestBid; }
    public double getMinIncrement() { return minIncrement; }
    public String getCurrentWinner() { return currentWinner; }
    public void setCurrentWinner(String currentWinner) { this.currentWinner = currentWinner; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public List<BidTransaction> getBidHistory() { return bidHistory; }
    public PriorityQueue<AutoBidConfig> getAutoBids() { return autoBids; }

    @Override
    public String getSummary() {
        return String.format("%s | Giá hiện tại: %.0f | Dẫn đầu: %s | Trạng thái: %s",
                itemName, currentHighestBid, currentWinner, status);
    }
}