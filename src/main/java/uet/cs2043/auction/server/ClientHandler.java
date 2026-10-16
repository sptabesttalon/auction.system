package uet.cs2043.auction.server;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import uet.cs2043.auction.common.protocol.SocketMessage;
import uet.cs2043.auction.server.controller.AuctionManager;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {
    private static final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();
    private final Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final Gson gson = new Gson();

    public ClientHandler(Socket socket) {
        this.socket = socket;
        activeClients.add(this);
    }

    public static void broadcast(String jsonMessage) {
        for (ClientHandler client : activeClients) {
            client.sendMessage(jsonMessage);
        }
    }

    public void sendMessage(String jsonMessage) {
        if (writer != null) {
            writer.println(jsonMessage);
        }
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = reader.readLine()) != null) {
                SocketMessage request = gson.fromJson(line, SocketMessage.class);
                handleRequest(request);
            }
        } catch (IOException e) {
            System.out.println("Client ngắt kết nối: " + socket.getInetAddress());
        } finally {
            activeClients.remove(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void handleRequest(SocketMessage req) {
        AuctionManager manager = AuctionManager.getInstance();
        try {
            switch (req.getAction()) {
                case GET_AUCTIONS:
                    sendMessage(gson.toJson(new SocketMessage(
                            SocketMessage.Action.SUCCESS_RESPONSE,
                            gson.toJson(manager.getAllAuctions()),
                            "Danh sách phiên đấu giá")));
                    break;

                case PLACE_BID:
                    Map<String, String> bidData = gson.fromJson(req.getPayload(),
                            new TypeToken<Map<String, String>>(){}.getType());
                    manager.placeBid(
                            bidData.get("auctionId"),
                            bidData.get("username"),
                            Double.parseDouble(bidData.get("amount"))
                    );
                    break;

                case REGISTER_AUTOBID:
                    Map<String, String> autoData = gson.fromJson(req.getPayload(),
                            new TypeToken<Map<String, String>>(){}.getType());
                    manager.registerAutoBid(
                            autoData.get("auctionId"),
                            autoData.get("username"),
                            Double.parseDouble(autoData.get("maxBid")),
                            Double.parseDouble(autoData.get("increment"))
                    );
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            // Trả lỗi trực tiếp về riêng Client vừa thao tác sai (Exception Handling)
            sendMessage(gson.toJson(new SocketMessage(
                    SocketMessage.Action.ERROR_RESPONSE, "", e.getMessage())));
        }
    }
}