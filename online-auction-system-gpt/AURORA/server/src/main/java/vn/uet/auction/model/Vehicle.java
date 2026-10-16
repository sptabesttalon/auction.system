package vn.uet.auction.model;

public final class Vehicle extends Item {
    private static final long serialVersionUID = 1L;
    public Vehicle(long id, String title, String description) { super(id, title, description, "🚗"); }
    @Override public String category() { return "VEHICLE"; }
}
