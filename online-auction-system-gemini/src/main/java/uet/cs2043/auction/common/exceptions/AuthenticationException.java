package uet.cs2043.auction.common.exceptions;

// Ngoại lệ khi xác thực thông tin đăng nhập/tài khoản thất bại
public class AuthenticationException extends Exception {
    public AuthenticationException(String message) {
        super(message);
    }
}