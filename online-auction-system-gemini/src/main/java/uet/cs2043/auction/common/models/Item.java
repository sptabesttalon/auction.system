package uet.cs2043.auction.common.models;

public abstract class Item extends Entity {
    private static final long serialVersionUID = 1L;
    private String name;
    private String description;
    private double startingPrice;
    private String category;

    public Item(String name, String description, double startingPrice, String category) {
        super();
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.category = category;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartingPrice() { return startingPrice; }
    public String getCategory() { return category; }

    // Đa hình (Polymorphism): các lớp con override phương thức này
    public abstract String printInfo();

    @Override
    public String getSummary() {
        return printInfo();
    }
}