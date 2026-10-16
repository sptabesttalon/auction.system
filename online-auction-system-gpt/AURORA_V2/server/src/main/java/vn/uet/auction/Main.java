package vn.uet.auction;

import vn.uet.auction.service.AuctionService;
import vn.uet.auction.storage.*;
import vn.uet.auction.web.ApiServer;
import java.nio.file.Path;
import java.time.Clock;

public final class Main {
    private Main(){ }
    public static void main(String[] args) throws Exception {
        String host=System.getenv().getOrDefault("AUCTION_HOST","127.0.0.1");
        int port=Integer.parseInt(System.getenv().getOrDefault("AUCTION_PORT","8080"));
        String backend=System.getenv().getOrDefault("AUCTION_STORAGE","file");
        Repository repo=backend.equalsIgnoreCase("sqlite")
            ?new SqliteRepository(System.getenv().getOrDefault("AUCTION_DB","auction.sqlite"))
            :new FileRepository(Path.of(System.getenv().getOrDefault("AUCTION_DATA","data/auction.dat")));
        AuctionService service=AuctionService.singleton(repo,Clock.systemUTC());
        service.bootstrapAdmin(System.getenv("AUCTION_ADMIN_USER"),System.getenv("AUCTION_ADMIN_PASSWORD"));
        if ("1".equals(System.getenv("AUCTION_DEMO"))) DemoSeeder.seed(service);
        ApiServer web=new ApiServer(service,host,port);
        Runtime.getRuntime().addShutdownHook(new Thread(web::close));
        web.start();
        System.out.println("Auction UET running at http://"+host+":"+web.port());
    }
}
