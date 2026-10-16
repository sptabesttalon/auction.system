package vn.uet.auction.storage;

import java.io.*;
import java.sql.*;

/** Optional SQLite JDBC DAO: one transactionally replaced serialized snapshot. */
public final class SqliteRepository implements Repository {
    private final String url;
    public SqliteRepository(String file) { this.url = "jdbc:sqlite:" + file; }
    private Connection open() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement stmt = c.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS snapshots (id INTEGER PRIMARY KEY, payload BLOB NOT NULL)");
        }
        return c;
    }
    @Override public State load() throws Exception {
        try (Connection c = open(); PreparedStatement p = c.prepareStatement("SELECT payload FROM snapshots WHERE id=1"); ResultSet r = p.executeQuery()) {
            if (!r.next()) return new State();
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(r.getBytes(1)))) {
                return (State) in.readObject();
            }
        }
    }
    @Override public void save(State state) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(state); }
        try (Connection c = open()) {
            c.setAutoCommit(false);
            try (PreparedStatement p = c.prepareStatement("INSERT INTO snapshots (id,payload) VALUES (1,?) ON CONFLICT(id) DO UPDATE SET payload=excluded.payload")) {
                p.setBytes(1, bytes.toByteArray()); p.executeUpdate(); c.commit();
            } catch (Exception e) { c.rollback(); throw e; }
        }
    }
}
