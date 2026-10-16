package vn.uet.auction.service;
public final class AuthenticationException extends ServiceException {
    public AuthenticationException(String msg) { super(401, msg); }
}
