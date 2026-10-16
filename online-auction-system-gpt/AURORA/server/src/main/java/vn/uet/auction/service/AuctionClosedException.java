package vn.uet.auction.service;
public final class AuctionClosedException extends ServiceException {
    public AuctionClosedException() { super(409, "Phiên đấu giá không còn hoạt động"); }
}
