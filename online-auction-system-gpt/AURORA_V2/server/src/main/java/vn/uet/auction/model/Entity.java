package vn.uet.auction.model;

import java.io.Serializable;

public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;

    protected Entity(long id) { this.id = id; }
    public final long getId() { return id; }
    public abstract String summary();
}
