package uet.cs2043.auction.common.exceptions;

// Ngoại lệ khi người dùng cố tình thao tác lúc phiên đấu giá đã kết thúc hoặc bị đóng
public class AuctionClosedException extends Exception {
    public AuctionClosedException(String message) {
        super(message);
    }
}