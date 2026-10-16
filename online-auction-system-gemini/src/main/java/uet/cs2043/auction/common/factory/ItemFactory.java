package uet.cs2043.auction.common.factory;

import uet.cs2043.auction.common.models.Item;

public class ItemFactory {

    public static class Electronics extends Item {
        private int warrantyMonths;
        public Electronics(String name, String desc, double price, int warrantyMonths) {
            super(name, desc, price, "ELECTRONICS");
            this.warrantyMonths = warrantyMonths;
        }
        @Override
        public String printInfo() {
            return String.format("[Điện tử] %s - Bảo hành: %d tháng - Khởi điểm: %.0f VNĐ",
                    getName(), warrantyMonths, getStartingPrice());
        }
    }

    public static class Art extends Item {
        private String artist;
        public Art(String name, String desc, double price, String artist) {
            super(name, desc, price, "ART");
            this.artist = artist;
        }
        @Override
        public String printInfo() {
            return String.format("[Nghệ thuật] %s - Tác giả: %s - Khởi điểm: %.0f VNĐ",
                    getName(), artist, getStartingPrice());
        }
    }

    public static class Vehicle extends Item {
        private int manufactureYear;
        public Vehicle(String name, String desc, double price, int manufactureYear) {
            super(name, desc, price, "VEHICLE");
            this.manufactureYear = manufactureYear;
        }
        @Override
        public String printInfo() {
            return String.format("[Phương tiện] %s - Sản xuất: %d - Khởi điểm: %.0f VNĐ",
                    getName(), manufactureYear, getStartingPrice());
        }
    }

    // Factory Method tạo đối tượng theo loại
    public static Item createItem(String type, String name, String desc, double price, String extraAttr) {
        switch (type.toUpperCase()) {
            case "ELECTRONICS":
                int warranty = extraAttr.isEmpty() ? 12 : Integer.parseInt(extraAttr);
                return new Electronics(name, desc, price, warranty);
            case "ART":
                return new Art(name, desc, price, extraAttr.isEmpty() ? "Khuyết danh" : extraAttr);
            case "VEHICLE":
                int year = extraAttr.isEmpty() ? 2024 : Integer.parseInt(extraAttr);
                return new Vehicle(name, desc, price, year);
            default:
                throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + type);
        }
    }
}