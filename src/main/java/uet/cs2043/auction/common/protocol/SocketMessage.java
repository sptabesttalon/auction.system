package uet.cs2043.auction.common.protocol;

import java.io.Serializable;

// Định dạng bản tin chuẩn truyền qua lại giữa Client và Server qua TCP Socket
public class SocketMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Action {
        LOGIN, REGISTER, GET_AUCTIONS, CREATE_AUCTION,
        PLACE_BID, REGISTER_AUTOBID,
        // Các sự kiện Server chủ động đẩy về (Observer / Realtime Push)
        AUCTION_UPDATED, AUCTION_CLOSED, ERROR_RESPONSE, SUCCESS_RESPONSE
    }

    private Action action;
    private String payload; // Dữ liệu đối tượng được serialize thành chuỗi JSON
    private String message;

    public SocketMessage(Action action, String payload, String message) {
        this.action = action;
        this.payload = payload;
        this.message = message;
    }

    public Action getAction() { return action; }
    public String getPayload() { return payload; }
    public String getMessage() { return message; }
}