package uet.cs2043.auction.common.models;

import java.io.Serializable;

public class BidTransaction extends Entity {
    private static final long serialVersionUID = 1L;
    private String auctionId;
    private String bidderUsername;
    private double amount;
    private long timestamp;

    public BidTransaction(String auctionId, String bidderUsername, double amount) {
        super();
        this.auctionId = auctionId;
        this.bidderUsername = bidderUsername;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
    }

    public String getAuctionId() { return auctionId; }
    public String getBidderUsername() { return bidderUsername; }
    public double getAmount() { return amount; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String getSummary() {
        return bidderUsername + " đặt " + amount + " lúc " + timestamp;
    }
}