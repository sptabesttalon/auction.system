package vn.uet.auction.model;

public abstract class Item extends Entity {
    private static final long serialVersionUID = 1L;
    private String title;
    private String description;
    private final String icon;

    protected Item(long id, String title, String description, String icon) {
        super(id); this.title = title; this.description = description; this.icon = icon;
    }
    public String title() { return title; }
    public String description() { return description; }
    public String icon() { return icon; }
    public abstract String category();
    public void edit(String title, String description) { this.title = title; this.description = description; }
    @Override public String summary() { return category() + ": " + title; }
}
