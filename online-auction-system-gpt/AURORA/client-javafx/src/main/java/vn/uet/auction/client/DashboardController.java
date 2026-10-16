package vn.uet.auction.client;

import com.google.gson.*;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.*;
import java.util.concurrent.*;

/** JavaFX Controller: translates user actions to client API calls; never edits server model. */
public final class DashboardController {
    @FXML private TableView<AuctionRow> auctionsTable;
    @FXML private TableColumn<AuctionRow,Long> idColumn;
    @FXML private TableColumn<AuctionRow,String> titleColumn,priceColumn,statusColumn;
    @FXML private TextField username,bidAmount,maxBid,increment,newTitle,newDescription,newPrice,newMinutes,editTitle;
    @FXML private PasswordField password;
    @FXML private ComboBox<String> role,newCategory;
    @FXML private Label currentUser,notification,itemTitle,description,priceLabel,auctionMeta;
    @FXML private TextArea editDescription;
    @FXML private LineChart<String,Number> chart;
    @FXML private ListView<String> bidHistory;
    private final ApiClient api=new ApiClient();
    private final ObservableList<AuctionRow> rows=FXCollections.observableArrayList();
    private final ExecutorService workers=Executors.newFixedThreadPool(3, r->{Thread t=new Thread(r);t.setDaemon(true);return t;});
    private JsonObject currentUserJson;
    private long selectedId=-1;
    private final java.text.NumberFormat money=java.text.NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
    @FXML private void initialize(){
        role.getItems().setAll("BIDDER","SELLER");role.setValue("BIDDER");
        newCategory.getItems().setAll("ELECTRONICS","ART","VEHICLE");newCategory.setValue("ELECTRONICS");
        newMinutes.setText("60");increment.setText("10000");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        auctionsTable.setItems(rows);
        auctionsTable.getSelectionModel().selectedItemProperty().addListener((obs,oldValue,newValue)->{
            if(newValue!=null)loadDetail(newValue.getId());
        });
        api.subscribe(event->Platform.runLater(this::refresh));
        refresh();
    }
    private void run(Callable<JsonElement> job,java.util.function.Consumer<JsonElement> success){
        workers.submit(()->{
            try{JsonElement result=job.call();Platform.runLater(()->success.accept(result));}
            catch(Exception ex){Platform.runLater(()->alert(ex.getMessage()));}
        });
    }
    private void alert(String msg){notification.setText(msg);new Alert(Alert.AlertType.INFORMATION,msg,ButtonType.OK).showAndWait();}
    private String money(long value){return money.format(value)+" ₫";}
    private static String chartTime(long epochMillis){
        return java.time.Instant.ofEpochMilli(epochMillis).atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }
    @FXML private void refresh(){
        run(()->api.send("GET","/auctions",null),data->{
            List<AuctionRow> next=new ArrayList<>();
            for(JsonElement entry:data.getAsJsonArray())next.add(new AuctionRow(entry.getAsJsonObject()));
            rows.setAll(next);notification.setText("Đã kết nối server · "+next.size()+" phiên · Nhận sự kiện SSE realtime");
            if(selectedId>=0)loadDetail(selectedId);
        });
    }
    @FXML private void register(){
        String name=username.getText().trim();String pass=password.getText();
        run(()->api.send("POST","/auth/register",ApiClient.json("username",name,"password",pass,"role",role.getValue())),result->{
            alert("Tạo tài khoản thành công. Nhấn Đăng nhập để bắt đầu.");
        });
    }
    @FXML private void login(){
        String name=username.getText().trim();String pass=password.getText();
        run(()->api.send("POST","/auth/login",ApiClient.json("username",name,"password",pass)),data->{
            JsonObject response=data.getAsJsonObject();api.setToken(response.get("token").getAsString());
            currentUserJson=response.getAsJsonObject("user");
            currentUser.setText(currentUserJson.get("username").getAsString()+" · "+currentUserJson.get("role").getAsString());
            password.clear();notification.setText("Đăng nhập thành công");
        });
    }
    @FXML private void logout(){
        run(()->api.send("POST","/auth/logout",null),result->{
            api.setToken("");currentUserJson=null;currentUser.setText("Chưa đăng nhập");
            notification.setText("Đã đăng xuất");
        });
    }
    private void loadDetail(long id){
        selectedId=id;
        run(()->api.send("GET","/auctions/"+id,null),data->{
            if(selectedId!=id)return;
            JsonObject a=data.getAsJsonObject();
            itemTitle.setText(a.get("icon").getAsString()+"  "+a.get("title").getAsString());
            description.setText(a.get("description").getAsString());
            priceLabel.setText(money(a.get("currentPrice").getAsLong()));
            String leader=a.get("leader").isJsonNull()?"Chưa có":a.get("leader").getAsString();
            auctionMeta.setText("#"+id+" · "+a.get("status").getAsString()+" · Người bán: "+a.get("seller").getAsString()+
                " · Dẫn đầu: "+leader+"\nKết thúc: "+java.time.Instant.ofEpochMilli(a.get("endTime").getAsLong()).atZone(java.time.ZoneId.systemDefault()));
            editTitle.setText(a.get("title").getAsString());editDescription.setText(a.get("description").getAsString());
            long price=a.get("currentPrice").getAsLong();bidAmount.setText(String.valueOf(price+10000));maxBid.setText(String.valueOf(price+100000));
            XYChart.Series<String,Number> series=new XYChart.Series<>();
            series.getData().add(new XYChart.Data<>(chartTime(a.get("startTime").getAsLong()),a.get("startPrice").getAsLong()));
            bidHistory.getItems().clear();
            int step=0;
            for(JsonElement entry:a.getAsJsonArray("history")){
                JsonObject h=entry.getAsJsonObject();step++;
                series.getData().add(new XYChart.Data<>(chartTime(h.get("createdAt").getAsLong()),h.get("amount").getAsLong()));
                bidHistory.getItems().add(0,"#"+h.get("id").getAsLong()+"  "+h.get("bidder").getAsString()+
                    "  → "+money(h.get("amount").getAsLong())+(h.get("automatic").getAsBoolean()?" ⚡":""));
            }
            chart.getData().setAll(series);
        });
    }
    private void requireSelection(){if(selectedId<0)throw new IllegalArgumentException("Chọn một phiên trước khi thao tác");}
    private void doSelected(String verb,String suffix,JsonObject data){
        try{
            requireSelection();long id=selectedId;
            run(()->api.send(verb,"/auctions/"+id+suffix,data),result->{notification.setText("Thao tác thành công");refresh();});
        }catch(Exception ex){alert(ex.getMessage());}
    }
    @FXML private void bid(){
        try{doSelected("POST","/bid",ApiClient.json("amount",Long.parseLong(bidAmount.getText().trim())));}
        catch(NumberFormatException ex){alert("Giá đấu phải là số nguyên VND");}
    }
    @FXML private void autoBid(){
        try{doSelected("POST","/auto",ApiClient.json("maxAmount",Long.parseLong(maxBid.getText().trim()),"increment",Long.parseLong(increment.getText().trim())));}
        catch(NumberFormatException ex){alert("Giá tối đa và bước giá phải là số nguyên");}
    }
    @FXML private void create(){
        try{
            long price=Long.parseLong(newPrice.getText().trim()),mins=Long.parseLong(newMinutes.getText().trim());
            if(mins<1||mins>365L*24*60)throw new IllegalArgumentException("Thời lượng từ 1 phút đến 365 ngày");
            long start=System.currentTimeMillis();
            JsonObject data=ApiClient.json("category",newCategory.getValue(),"title",newTitle.getText(),"description",newDescription.getText(),"startPrice",price,"startTime",start,"endTime",start+mins*60000);
            run(()->api.send("POST","/auctions",data),result->{notification.setText("Đã tạo phiên mới");refresh();});
        }catch(NumberFormatException ex){alert("Giá và thời lượng phải là số nguyên");}
         catch(Exception ex){alert(ex.getMessage());}
    }
    @FXML private void edit(){doSelected("PUT","",ApiClient.json("title",editTitle.getText(),"description",editDescription.getText()));}
    @FXML private void delete(){doSelected("DELETE","",null);selectedId=-1;}
    @FXML private void cancel(){doSelected("POST","/cancel",null);}
    @FXML private void pay(){doSelected("POST","/pay",null);}
}
