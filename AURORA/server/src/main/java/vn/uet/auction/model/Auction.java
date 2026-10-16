package vn.uet.auction.model;

import java.util.ArrayList;
import java.util.List;

public final class Auction extends Entity {
    private static final long serialVersionUID = 1L;
    public enum Status { OPEN, RUNNING, FINISHED, PAID, CANCELED }
    public final long sellerId;
    public final Item item;
    public final long startPrice;
    public long currentPrice;
    public long startTime;
    public long endTime;
    public Long leaderId;
    public Status status;
    public long version;
    public final List<BidTransaction> history = new ArrayList<>();
    public final List<AutoBid> autoBids = new ArrayList<>();

    public Auction(long id, long sellerId, Item item, long startPrice, long startTime, long endTime) {
        super(id);
        this.sellerId = sellerId; this.item = item; this.startPrice = startPrice;
        this.currentPrice = startPrice; this.startTime = startTime; this.endTime = endTime;
        this.status = Status.OPEN;
    }
    @Override public String summary() { return item.summary() + ", hiện tại " + currentPrice; }
}
