package uet.cs2043.auction.common.exceptions;

// Ngoại lệ khi người dùng đặt giá không hợp lệ (nhỏ hơn giá sàn/bước giá quy định)
public class InvalidBidException extends Exception {
    public InvalidBidException(String message) {
        super(message);
    }
}