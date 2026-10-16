package vn.uet.auction.model;

public final class Admin extends User {
    private static final long serialVersionUID = 1L;
    public Admin(long id, String username, String salt, String passwordHash) {
        super(id, username, salt, passwordHash);
    }
    @Override public String role() { return "ADMIN"; }
}
