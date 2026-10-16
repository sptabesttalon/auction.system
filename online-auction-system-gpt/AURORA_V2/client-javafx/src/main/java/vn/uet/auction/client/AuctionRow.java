package vn.uet.auction.client;

import com.google.gson.JsonObject;
import java.text.NumberFormat;
import java.util.Locale;

public final class AuctionRow {
    private final JsonObject raw;
    public AuctionRow(JsonObject raw){this.raw=raw;}
    public long getId(){return raw.get("id").getAsLong();}
    public String getTitle(){return raw.get("title").getAsString();}
    public String getCategory(){return raw.get("category").getAsString();}
    public String getStatus(){return raw.get("status").getAsString();}
    public String getSeller(){return raw.get("seller").getAsString();}
    public long getPriceValue(){return raw.get("currentPrice").getAsLong();}
    public String getPrice(){return NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")).format(getPriceValue())+" ₫";}
    public JsonObject raw(){return raw;}
}
