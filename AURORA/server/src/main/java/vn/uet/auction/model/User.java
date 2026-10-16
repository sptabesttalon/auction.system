package vn.uet.auction.model;

public abstract class User extends Entity {
    private static final long serialVersionUID = 1L;
    private final String username;
    private final String salt;
    private final String passwordHash;

    protected User(long id, String username, String salt, String passwordHash) {
        super(id);
        this.username = username;
        this.salt = salt;
        this.passwordHash = passwordHash;
    }
    public String getUsername() { return username; }
    public String getSalt() { return salt; }
    public String getPasswordHash() { return passwordHash; }
    public abstract String role();
    @Override public String summary() { return role() + ": " + username; }
}
