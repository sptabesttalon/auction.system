package vn.uet.auction.model;

public final class Bidder extends User {
    private static final long serialVersionUID = 1L;
    public Bidder(long id, String username, String salt, String passwordHash) {
        super(id, username, salt, passwordHash);
    }
    @Override public String role() { return "BIDDER"; }
}
