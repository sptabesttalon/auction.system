package vn.uet.auction.storage;

public interface Repository {
    State load() throws Exception;
    void save(State state) throws Exception;
}
