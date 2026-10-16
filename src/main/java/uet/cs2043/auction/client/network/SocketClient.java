package uet.cs2043.auction.client.network;

import com.google.gson.Gson;
import uet.cs2043.auction.common.models.Auction;
import uet.cs2043.auction.common.protocol.SocketMessage;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class SocketClient {
    private static SocketClient instance;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final Gson gson = new Gson();

    private Consumer<Auction> onAuctionUpdatedCallback;
    private Consumer<String> onErrorCallback;
    private Consumer<String> onSuccessCallback;

    private SocketClient() {}

    public static synchronized SocketClient getInstance() {
        if (instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    public void connect(String host, int port) throws IOException {
        if (socket != null && !socket.isClosed()) return;
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);

        // Luồng chạy ngầm liên tục lắng nghe bản tin đẩy về từ Server (Push Event)
        Thread listenThread = new Thread(this::listenServer);
        listenThread.setDaemon(true);
        listenThread.start();
    }

    private void listenServer() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                SocketMessage msg = gson.fromJson(line, SocketMessage.class);
                switch (msg.getAction()) {
                    case AUCTION_UPDATED:
                    case AUCTION_CLOSED:
                        if (onAuctionUpdatedCallback != null) {
                            Auction auction = gson.fromJson(msg.getPayload(), Auction.class);
                            onAuctionUpdatedCallback.accept(auction);
                        }
                        break;
                    case ERROR_RESPONSE:
                        if (onErrorCallback != null) {
                            onErrorCallback.accept(msg.getMessage());
                        }
                        break;
                    case SUCCESS_RESPONSE:
                        if (onSuccessCallback != null) {
                            onSuccessCallback.accept(msg.getPayload());
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("Mất kết nối tới Server: " + e.getMessage());
        }
    }

    public void sendMessage(SocketMessage msg) {
        if (writer != null) {
            writer.println(gson.toJson(msg));
        }
    }

    public void setOnAuctionUpdated(Consumer<Auction> callback) { this.onAuctionUpdatedCallback = callback; }
    public void setOnError(Consumer<String> callback) { this.onErrorCallback = callback; }
    public void setOnSuccess(Consumer<String> callback) { this.onSuccessCallback = callback; }
}