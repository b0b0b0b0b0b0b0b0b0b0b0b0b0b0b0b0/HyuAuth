package bm.b0b0b0.hyuauth.database;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class DatabaseManager {
    private final String databaseUrl;
    private Connection connection;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }
    }

    public DatabaseManager(Path dataDirectory, String dbFileName) {
        this.databaseUrl = "jdbc:sqlite:" + dataDirectory.resolve(dbFileName).toString();
        System.out.println("[HyuAuth] Database path: " + this.databaseUrl);
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            System.out.println("[HyuAuth] Connecting to SQLite database...");
            connection = DriverManager.getConnection(databaseUrl);
            System.out.println("[HyuAuth] Database connection established");
            createTables();
            System.out.println("[HyuAuth] Database tables initialized");
        } catch (SQLException e) {
            System.out.println("[HyuAuth] ERROR: Failed to initialize database: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void createTables() throws SQLException {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS users (
                uuid TEXT PRIMARY KEY,
                password_hash TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("[HyuAuth] Table 'users' ready");
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(databaseUrl);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get database connection", e);
        }
        return connection;
    }

    public boolean isUserRegistered(UUID uuid) {
        String sql = "SELECT COUNT(*) FROM users WHERE uuid = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check user registration", e);
        }
        return false;
    }

    public String getPasswordHash(UUID uuid) {
        String sql = "SELECT password_hash FROM users WHERE uuid = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password_hash");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get password hash", e);
        }
        return null;
    }

    public void registerUser(UUID uuid, String passwordHash) {
        String sql = "INSERT INTO users (uuid, password_hash, created_at) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, passwordHash);
            pstmt.setLong(3, System.currentTimeMillis());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to register user", e);
        }
    }

    public void updatePassword(UUID uuid, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE uuid = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, passwordHash);
            pstmt.setString(2, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update password", e);
        }
    }

    public void deleteUser(UUID uuid) {
        String sql = "DELETE FROM users WHERE uuid = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                System.out.println("[HyuAuth] Closing database connection...");
                connection.close();
                System.out.println("[HyuAuth] Database connection closed");
            }
        } catch (SQLException e) {
            System.out.println("[HyuAuth] ERROR: Failed to close database connection: " + e.getMessage());
            throw new RuntimeException("Failed to close database connection", e);
        }
    }
}
