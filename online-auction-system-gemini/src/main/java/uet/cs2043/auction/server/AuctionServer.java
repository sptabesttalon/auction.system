package uet.cs2043.auction.server;

import com.google.gson.Gson;
import uet.cs2043.auction.common.models.Auction;
import uet.cs2043.auction.common.protocol.SocketMessage;
import uet.cs2043.auction.server.controller.AuctionManager;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionServer implements AuctionManager.AuctionObserver {
    private static final int PORT = 5000;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final Gson gson = new Gson();

    public void start() {
        AuctionManager manager = AuctionManager.getInstance();
        manager.addObserver(this);

        // Tạo sẵn 1 phiên đấu giá mẫu để demo ngay khi bật Server
        manager.createAuction("MacBook Pro M3 Max", "RAM 64GB, SSD 2TB", "seller_uet", 25000000, 500000, 300);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Auction Server đang lắng nghe tại cổng " + PORT + "...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                threadPool.execute(handler); // Sử dụng Thread Pool thay vì tạo Thread thủ công
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAuctionUpdated(Auction auction) {
        SocketMessage msg = new SocketMessage(
                SocketMessage.Action.AUCTION_UPDATED,
                gson.toJson(auction),
                "Cập nhật giá mới realtime!"
        );
        ClientHandler.broadcast(gson.toJson(msg));
    }

    @Override
    public void onAuctionClosed(Auction auction) {
        SocketMessage msg = new SocketMessage(
                SocketMessage.Action.AUCTION_CLOSED,
                gson.toJson(auction),
                "Phiên đấu giá đã kết thúc! Người thắng: " + auction.getCurrentWinner()
        );
        ClientHandler.broadcast(gson.toJson(msg));
    }

    public static void main(String[] args) {
        new AuctionServer().start();
    }
}