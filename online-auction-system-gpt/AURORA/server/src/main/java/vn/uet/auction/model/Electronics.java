package vn.uet.auction.model;

public final class Electronics extends Item {
    private static final long serialVersionUID = 1L;
    public Electronics(long id, String title, String description) { super(id, title, description, "💻"); }
    @Override public String category() { return "ELECTRONICS"; }
}
