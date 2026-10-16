package vn.uet.auction.model;

public final class Seller extends User {
    private static final long serialVersionUID = 1L;
    public Seller(long id, String username, String salt, String passwordHash) {
        super(id, username, salt, passwordHash);
    }
    @Override public String role() { return "SELLER"; }
}
