package uet.cs2043.auction.client.controllers;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import uet.cs2043.auction.client.network.SocketClient;
import uet.cs2043.auction.common.models.Auction;
import uet.cs2043.auction.common.models.BidTransaction;
import uet.cs2043.auction.common.protocol.SocketMessage;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainAuctionController {
    @FXML private Label lblItemName;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblWinner;
    @FXML private Label lblCountdown;
    @FXML private TextField txtBidAmount;
    @FXML private TextField txtMaxAutoBid;
    @FXML private TextField txtAutoIncrement;
    @FXML private LineChart<String, Number> priceLineChart;

    private final XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private final Gson gson = new Gson();

    private String currentUsername;
    private String currentAuctionId;

    @FXML
    public void initialize() {
        priceSeries.setName("Lịch sử giá (VNĐ)");
        priceLineChart.getData().add(priceSeries);
        priceLineChart.setAnimated(false);

        // Đăng ký nhận sự kiện cập nhật Realtime từ Server
        SocketClient.getInstance().setOnAuctionUpdated(this::onRealtimeAuctionUpdate);
        SocketClient.getInstance().setOnError(err -> 
            Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, err).showAndWait())
        );

        // Gửi yêu cầu lấy danh sách phiên đấu giá hiện có
        SocketClient.getInstance().sendMessage(
            new SocketMessage(SocketMessage.Action.GET_AUCTIONS, "", "")
        );
    }

    public void setCurrentUser(String username) {
        this.currentUsername = username;
    }

    public void onRealtimeAuctionUpdate(Auction updatedAuction) {
        Platform.runLater(() -> {
            this.currentAuctionId = updatedAuction.getId();
            lblItemName.setText(updatedAuction.getItemName() + " (" + updatedAuction.getItemDescription() + ")");
            lblCurrentPrice.setText(String.format("%,.0f VNĐ", updatedAuction.getCurrentHighestBid()));
            lblWinner.setText("Dẫn đầu: " + updatedAuction.getCurrentWinner());
            lblCountdown.setText("Trạng thái: " + updatedAuction.getStatus());

            // Vẽ lại đường biểu đồ giá
            priceSeries.getData().clear();
            for (BidTransaction bid : updatedAuction.getBidHistory()) {
                String timeLabel = timeFormat.format(new Date(bid.getTimestamp()));
                priceSeries.getData().add(new XYChart.Series.Data<>(timeLabel, bid.getAmount()));
            }
        });
    }

    @FXML
    private void handlePlaceBid(ActionEvent event) {
        try {
            double amount = Double.parseDouble(txtBidAmount.getText().trim());
            Map<String, String> payload = new HashMap<>();
            payload.put("auctionId", currentAuctionId);
            payload.put("username", currentUsername);
            payload.put("amount", String.valueOf(amount));

            SocketClient.getInstance().sendMessage(new SocketMessage(
                SocketMessage.Action.PLACE_BID,
                gson.toJson(payload),
                ""
            ));
            txtBidAmount.clear();
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING, "Vui lòng nhập số tiền hợp lệ!").showAndWait();
        }
    }

    @FXML
    private void handleRegisterAutoBid(ActionEvent event) {
        try {
            double maxBid = Double.parseDouble(txtMaxAutoBid.getText().trim());
            double increment = Double.parseDouble(txtAutoIncrement.getText().trim());

            Map<String, String> payload = new HashMap<>();
            payload.put("auctionId", currentAuctionId);
            payload.put("username", currentUsername);
            payload.put("maxBid", String.valueOf(maxBid));
            payload.put("increment", String.valueOf(increment));

            SocketClient.getInstance().sendMessage(new SocketMessage(
                SocketMessage.Action.REGISTER_AUTOBID,
                gson.toJson(payload),
                ""
            ));
            txtMaxAutoBid.clear();
            txtAutoIncrement.clear();
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING, "Vui lòng nhập đúng định dạng số!").showAndWait();
        }
    }
}