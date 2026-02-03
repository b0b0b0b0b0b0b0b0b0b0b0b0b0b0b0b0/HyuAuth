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
                username TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN username TEXT");
            } catch (SQLException e) {
            }
            System.out.println("[HyuAuth] Table 'users' ready");
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                if (connection != null && !connection.isClosed()) {
                    try {
                        connection.close();
                    } catch (SQLException ignored) {
                    }
                }
                connection = DriverManager.getConnection(databaseUrl);
                System.out.println("[HyuAuth] Database connection reestablished");
            }
        } catch (SQLException e) {
            System.out.println("[HyuAuth] ERROR: Failed to get database connection: " + e.getMessage());
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException ignored) {
            }
            try {
                connection = DriverManager.getConnection(databaseUrl);
                System.out.println("[HyuAuth] Database connection recovered after error");
            } catch (SQLException e2) {
                System.out.println("[HyuAuth] CRITICAL: Failed to recover database connection: " + e2.getMessage());
                throw new RuntimeException("Failed to get database connection", e2);
            }
        }
        return connection;
    }

    public boolean isUserRegistered(UUID uuid) {
        String sql = "SELECT COUNT(*) FROM users WHERE uuid = ?";
        int retries = 3;
        while (retries > 0) {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
                return false;
            } catch (SQLException e) {
                retries--;
                System.out.println("[HyuAuth] ERROR: Failed to check user registration (retries left: " + retries + "): " + e.getMessage());
                if (retries > 0) {
                    try {
                        Thread.sleep(100);
                        getConnection();
                    } catch (InterruptedException | RuntimeException ignored) {
                    }
                } else {
                    throw new RuntimeException("Failed to check user registration after retries", e);
                }
            }
        }
        return false;
    }

    public String getPasswordHash(UUID uuid) {
        String sql = "SELECT password_hash FROM users WHERE uuid = ?";
        int retries = 3;
        while (retries > 0) {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("password_hash");
                    }
                }
                return null;
            } catch (SQLException e) {
                retries--;
                System.out.println("[HyuAuth] ERROR: Failed to get password hash (retries left: " + retries + "): " + e.getMessage());
                if (retries > 0) {
                    try {
                        Thread.sleep(100);
                        getConnection();
                    } catch (InterruptedException | RuntimeException ignored) {
                    }
                } else {
                    throw new RuntimeException("Failed to get password hash after retries", e);
                }
            }
        }
        return null;
    }

    public void registerUser(UUID uuid, String username, String passwordHash) {
        String sql = "INSERT INTO users (uuid, username, password_hash, created_at) VALUES (?, ?, ?, ?)";
        int retries = 3;
        while (retries > 0) {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, username);
                pstmt.setString(3, passwordHash);
                pstmt.setLong(4, System.currentTimeMillis());
                pstmt.executeUpdate();
                return;
            } catch (SQLException e) {
                retries--;
                System.out.println("[HyuAuth] ERROR: Failed to register user (retries left: " + retries + "): " + e.getMessage());
                if (retries > 0) {
                    try {
                        Thread.sleep(100);
                        getConnection();
                    } catch (InterruptedException | RuntimeException ignored) {
                    }
                } else {
                    throw new RuntimeException("Failed to register user after retries", e);
                }
            }
        }
    }

    public void updatePassword(UUID uuid, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE uuid = ?";
        int retries = 3;
        while (retries > 0) {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setString(1, passwordHash);
                pstmt.setString(2, uuid.toString());
                pstmt.executeUpdate();
                return;
            } catch (SQLException e) {
                retries--;
                System.out.println("[HyuAuth] ERROR: Failed to update password (retries left: " + retries + "): " + e.getMessage());
                if (retries > 0) {
                    try {
                        Thread.sleep(100);
                        getConnection();
                    } catch (InterruptedException | RuntimeException ignored) {
                    }
                } else {
                    throw new RuntimeException("Failed to update password after retries", e);
                }
            }
        }
    }

    public void deleteUser(UUID uuid) {
        String sql = "DELETE FROM users WHERE uuid = ?";
        int retries = 3;
        while (retries > 0) {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.executeUpdate();
                return;
            } catch (SQLException e) {
                retries--;
                System.out.println("[HyuAuth] ERROR: Failed to delete user (retries left: " + retries + "): " + e.getMessage());
                if (retries > 0) {
                    try {
                        Thread.sleep(100);
                        getConnection();
                    } catch (InterruptedException | RuntimeException ignored) {
                    }
                } else {
                    throw new RuntimeException("Failed to delete user after retries", e);
                }
            }
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
