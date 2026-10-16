package vn.uet.auction.service;
public final class InvalidBidException extends ServiceException {
    public InvalidBidException(String msg) { super(422, msg); }
}
