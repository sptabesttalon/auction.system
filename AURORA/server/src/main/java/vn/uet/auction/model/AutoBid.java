package vn.uet.auction.model;

import java.io.Serializable;

public final class AutoBid implements Serializable {
    private static final long serialVersionUID = 1L;
    public final long bidderId;
    public final long maxAmount;
    public final long increment;
    public final long registeredAt;
    public final long registrationOrder;

    public AutoBid(long bidderId, long maxAmount, long increment, long registeredAt, long registrationOrder) {
        this.bidderId = bidderId;
        this.maxAmount = maxAmount;
        this.increment = increment;
        this.registeredAt = registeredAt;
        this.registrationOrder = registrationOrder;
    }
}
