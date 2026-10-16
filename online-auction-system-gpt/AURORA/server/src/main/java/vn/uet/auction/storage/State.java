package vn.uet.auction.storage;

import vn.uet.auction.model.Auction;
import vn.uet.auction.model.User;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public final class State implements Serializable {
    private static final long serialVersionUID = 1L;
    public long nextUserId = 1;
    public long nextAuctionId = 1;
    public long nextBidId = 1;
    public long nextAutoBidOrder = 1;
    public Map<Long, User> users = new LinkedHashMap<>();
    public Map<Long, Auction> auctions = new LinkedHashMap<>();
}
