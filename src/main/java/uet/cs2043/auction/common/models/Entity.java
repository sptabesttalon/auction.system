package uet.cs2043.auction.common.models;

import java.io.Serializable;
import java.util.UUID;

public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;
    protected String id;
    protected long createdAt;

    public Entity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
    }

    public Entity(String id) {
        this.id = id;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public long getCreatedAt() { return createdAt; }
    public abstract String getSummary();
}