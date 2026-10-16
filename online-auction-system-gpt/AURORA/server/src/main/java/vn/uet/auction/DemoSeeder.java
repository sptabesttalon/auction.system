package vn.uet.auction;

import vn.uet.auction.service.AuctionService;

/** Development-only demo fixtures. Never enable automatically in production. */
public final class DemoSeeder {
    private DemoSeeder() { }
    public static void seed(AuctionService svc) {
        if (!svc.list().isEmpty()) return;
        try {
            svc.register("seller_demo","demo12345","SELLER");
            svc.register("bidder_demo","demo12345","BIDDER");
        }catch(RuntimeException ignored){ }
        String seller=(String)svc.login("seller_demo","demo12345").get("token");
        long now=System.currentTimeMillis();
        svc.create(seller,"ELECTRONICS","MacBook Pro M3 Pro 14 inch","Laptop đồ họa cao cấp, RAM 18GB, SSD 512GB, ngoại hình đẹp.",32_000_000,now,now+86_400_000);
        svc.create(seller,"ART","Tranh sơn dầu Hoàng hôn","Tác phẩm nghệ thuật sơn dầu vẽ tay, kích thước 60 × 80cm.",2_500_000,now,now+43_200_000);
        svc.create(seller,"VEHICLE","Yamaha Exciter 155 VVA","Xe chính chủ, bản cao cấp, giấy tờ đầy đủ, bảo dưỡng định kỳ.",33_000_000,now,now+172_800_000);
        svc.create(seller,"ELECTRONICS","Máy ảnh Fujifilm X100V","Máy ảnh du lịch cao cấp, cảm biến APS-C, hoạt động hoàn hảo.",22_000_000,now,now+3600_000);
        String bidder=(String)svc.login("bidder_demo","demo12345").get("token");
        svc.bid(bidder,1,33_000_000);svc.bid(bidder,2,2_700_000);
    }
}
