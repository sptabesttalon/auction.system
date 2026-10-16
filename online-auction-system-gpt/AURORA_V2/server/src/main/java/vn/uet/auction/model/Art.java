package vn.uet.auction.model;

public final class Art extends Item {
    private static final long serialVersionUID = 1L;
    public Art(long id, String title, String description) { super(id, title, description, "🎨"); }
    @Override public String category() { return "ART"; }
}
