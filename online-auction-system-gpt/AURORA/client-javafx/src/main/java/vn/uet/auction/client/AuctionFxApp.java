package vn.uet.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Objects;

public final class AuctionFxApp extends Application {
    @Override public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/vn/uet/auction/client/dashboard.fxml")));
        Scene scene = new Scene(root, 1260, 800);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/vn/uet/auction/client/theme.css")).toExternalForm());
        stage.setTitle("AURORA • Online Auction — JavaFX Client");
        stage.setScene(scene);stage.setMinWidth(920);stage.setMinHeight(650);stage.show();
    }
    public static void main(String[] args) {launch(args);}
}
