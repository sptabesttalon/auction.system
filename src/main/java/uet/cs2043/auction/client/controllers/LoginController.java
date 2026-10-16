package uet.cs2043.auction.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import uet.cs2043.auction.client.network.SocketClient;

public class LoginController {
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMessage;

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = txtUsername.getText().trim();
        if (username.isEmpty()) {
            lblMessage.setText("Vui lòng nhập tên người dùng!");
            return;
        }

        try {
            // Kết nối Socket tới Server cổng 5000
            SocketClient client = SocketClient.getInstance();
            client.connect("127.0.0.1", 5000);

            // Chuyển sang màn hình Đấu giá chính
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/uet/cs2043/auction/views/main-auction-view.fxml"));
            Parent root = loader.load();

            MainAuctionController mainController = loader.getController();
            mainController.setCurrentUser(username);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Phiên Đấu Giá Trực Tuyến - Người dùng: " + username);
            stage.setScene(new Scene(root, 900, 650));
            stage.show();

        } catch (Exception e) {
            lblMessage.setText("Không thể kết nối tới Server cổng 5000!");
        }
    }
}