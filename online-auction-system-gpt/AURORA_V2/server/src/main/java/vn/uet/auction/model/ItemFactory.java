package vn.uet.auction.model;

/** GoF Factory Method: concrete creator subclasses override createItem(). */
public abstract class ItemFactory {
    public abstract Item createItem(long id, String title, String description);

    public static Item create(long id,String category,String title,String description) {
        if(category==null)throw new IllegalArgumentException("Thiếu loại sản phẩm");
        ItemFactory creator=switch(category.toUpperCase(java.util.Locale.ROOT)){
            case "ELECTRONICS" -> new ElectronicsFactory();
            case "ART" -> new ArtFactory();
            case "VEHICLE" -> new VehicleFactory();
            default -> throw new IllegalArgumentException("Loại sản phẩm không hợp lệ");
        };
        return creator.createItem(id,title,description);
    }
    private static final class ElectronicsFactory extends ItemFactory {
        @Override public Item createItem(long id,String t,String d){return new Electronics(id,t,d);}
    }
    private static final class ArtFactory extends ItemFactory {
        @Override public Item createItem(long id,String t,String d){return new Art(id,t,d);}
    }
    private static final class VehicleFactory extends ItemFactory {
        @Override public Item createItem(long id,String t,String d){return new Vehicle(id,t,d);}
    }
}
