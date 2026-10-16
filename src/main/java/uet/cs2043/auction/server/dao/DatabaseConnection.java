package uet.cs2043.auction.server.dao;

import uet.cs2043.auction.common.models.Auction;

import java.io.*;
import java.sql.*;
import java.util.Collection;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;
    private static final String URL = "jdbc:sqlite:auction_data.db";
    private static final String BACKUP_FILE = "auctions_snapshot.dat";

    private DatabaseConnection() throws SQLException {
        this.connection = DriverManager.getConnection(URL);
        initTables();
    }

    // Singleton Pattern cho kết nối Database (Theo đúng Slide trang 55)
    public static synchronized DatabaseConnection getInstance() throws SQLException {
        if (instance == null || instance.getConnection().isClosed()) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    private void initTables() throws SQLException {
        String createUsers = "CREATE TABLE IF NOT EXISTS users ("
                + "username TEXT PRIMARY KEY, "
                + "password TEXT NOT NULL, "
                + "role TEXT NOT NULL);";
        String createBids = "CREATE TABLE IF NOT EXISTS bid_logs ("
                + "id TEXT PRIMARY KEY, "
                + "auction_id TEXT, "
                + "bidder TEXT, "
                + "amount REAL, "
                + "timestamp INTEGER);";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUsers);
            stmt.execute(createBids);
        }
    }

    // Lưu trữ đối tượng bằng kỹ thuật Java Serialization (Theo Slide trang 44)
    public static void saveSnapshotToFile(Collection<Auction> auctions) {
        try (FileOutputStream fos = new FileOutputStream(BACKUP_FILE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(auctions.toArray(new Auction[0]));
        } catch (IOException e) {
            System.err.println("Lỗi lưu snapshot: " + e.getMessage());
        }
    }
}