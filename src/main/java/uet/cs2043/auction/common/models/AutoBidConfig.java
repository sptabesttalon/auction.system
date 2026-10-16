package uet.cs2043.auction.common.models;

import java.io.Serializable;

// Dùng cho PriorityQueue của chức năng nâng cao Auto-Bidding
public class AutoBidConfig implements Serializable, Comparable<AutoBidConfig> {
    private static final long serialVersionUID = 1L;
    private String bidderUsername;
    private double maxBid;
    private double increment;
    private long registeredAt;

    public AutoBidConfig(String bidderUsername, double maxBid, double increment) {
        this.bidderUsername = bidderUsername;
        this.maxBid = maxBid;
        this.increment = increment;
        this.registeredAt = System.nanoTime(); // Độ chính xác nano giây để ưu tiên người đăng ký trước
    }

    public String getBidderUsername() { return bidderUsername; }
    public double getMaxBid() { return maxBid; }
    public double getIncrement() { return increment; }
    public long getRegisteredAt() { return registeredAt; }

    @Override
    public int compareTo(AutoBidConfig other) {
        // Ưu tiên 1: Ai có maxBid cao hơn xếp trước (Giảm dần)
        int priceComp = Double.compare(other.maxBid, this.maxBid);
        if (priceComp != 0) {
            return priceComp;
        }
        // Ưu tiên 2: Nếu maxBid bằng nhau, ai đăng ký sớm hơn xếp trước (Tăng dần theo thời gian)
        return Long.compare(this.registeredAt, other.registeredAt);
    }
}