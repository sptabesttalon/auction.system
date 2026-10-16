package vn.uet.auction.service;

public class ServiceException extends RuntimeException {
    public final int statusCode;
    public ServiceException(int statusCode, String message) { super(message); this.statusCode = statusCode; }
}
