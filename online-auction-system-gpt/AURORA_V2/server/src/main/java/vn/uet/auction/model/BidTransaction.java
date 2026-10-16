package vn.uet.auction.model;

public final class BidTransaction extends Entity {
    private static final long serialVersionUID = 1L;
    public final long bidderId;
    public final long amount;
    public final long createdAt;
    public final boolean automatic;
    public BidTransaction(long id, long bidderId, long amount, long createdAt, boolean automatic) {
        super(id); this.bidderId = bidderId; this.amount = amount;
        this.createdAt = createdAt; this.automatic = automatic;
    }
    @Override public String summary() { return "Giá #" + getId() + ": " + amount; }
}
