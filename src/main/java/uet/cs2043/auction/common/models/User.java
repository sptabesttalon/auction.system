package uet.cs2043.auction.common.models;

public abstract class User extends Entity {
    private static final long serialVersionUID = 1L;
    public enum Role { BIDDER, SELLER, ADMIN }

    private String username;
    private String password;
    private Role role;

    public User(String id, String username, String password, Role role) {
        super(id);
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }

    @Override
    public String getSummary() {
        return String.format("[%s] %s", role.name(), username);
    }
}

// Các lớp con kế thừa User (Có thể tách thành từng file hoặc để chung package)
class Bidder extends User {
    public Bidder(String id, String username, String password) {
        super(id, username, password, Role.BIDDER);
    }
}

class Seller extends User {
    public Seller(String id, String username, String password) {
        super(id, username, password, Role.SELLER);
    }
}

class Admin extends User {
    public Admin(String id, String username, String password) {
        super(id, username, password, Role.ADMIN);
    }
}