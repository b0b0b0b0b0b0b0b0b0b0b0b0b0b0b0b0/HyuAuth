package bm.b0b0b0.hyuauth.manager;

import bm.b0b0b0.hyuauth.config.ConfigManager;
import bm.b0b0b0.hyuauth.database.DatabaseManager;
import bm.b0b0b0.hyuauth.security.PasswordHasher;
import com.hypixel.hytale.math.vector.Vector3d;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class AuthManager {
    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;
    private final Set<UUID> loggedInUsers;
    private final Map<UUID, Long> joinTimes;
    private final Map<UUID, Vector3d> joinLocations;

    public AuthManager(Path dataDirectory, ConfigManager configManager) {
        this.configManager = configManager;
        this.databaseManager = new DatabaseManager(dataDirectory, configManager.getDatabaseFileName());
        this.loggedInUsers = ConcurrentHashMap.newKeySet();
        this.joinTimes = new ConcurrentHashMap<>();
        this.joinLocations = new ConcurrentHashMap<>();
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public CompletableFuture<Boolean> isRegistered(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> databaseManager.isUserRegistered(uuid));
    }

    public boolean isLoggedIn(UUID uuid) {
        return loggedInUsers.contains(uuid);
    }

    public boolean register(UUID uuid, String username, String password) {
        if (databaseManager.isUserRegistered(uuid)) {
            return false;
        }
        String passwordHash = PasswordHasher.hash(password);
        databaseManager.registerUser(uuid, username, passwordHash);
        loggedInUsers.add(uuid);
        joinTimes.remove(uuid);
        joinLocations.remove(uuid);
        return true;
    }

    public boolean login(UUID uuid, String password) {
        if (!databaseManager.isUserRegistered(uuid)) {
            return false;
        }
        
        String storedHash = databaseManager.getPasswordHash(uuid);
        if (storedHash == null) {
            return false;
        }
        
        if (PasswordHasher.verify(password, storedHash)) {
            loggedInUsers.add(uuid);
            joinTimes.remove(uuid);
            joinLocations.remove(uuid);
            return true;
        }
        
        return false;
    }

    public void logout(UUID uuid) {
        loggedInUsers.remove(uuid);
        joinTimes.put(uuid, System.currentTimeMillis());
        joinLocations.remove(uuid);
    }

    public void markJoin(UUID uuid) {
        if (!isLoggedIn(uuid) && !joinTimes.containsKey(uuid)) {
            joinTimes.put(uuid, System.currentTimeMillis());
        }
    }

    public void markJoinLocation(UUID uuid, Vector3d position) {
        if (!joinLocations.containsKey(uuid)) {
            joinLocations.put(uuid, position);
        }
    }

    public Vector3d getJoinLocation(UUID uuid) {
        return joinLocations.get(uuid);
    }

    public void markQuit(UUID uuid) {
        loggedInUsers.remove(uuid);
        joinTimes.remove(uuid);
        joinLocations.remove(uuid);
    }

    public boolean shouldKick(UUID uuid) {
        if (isLoggedIn(uuid)) {
            return false;
        }
        
        Long joinTime = joinTimes.get(uuid);
        if (joinTime == null) {
            return false;
        }
        
        long elapsedSeconds = (System.currentTimeMillis() - joinTime) / 1000L;
        return elapsedSeconds > configManager.getLoginTimeoutSeconds();
    }

    public void setTimeout(int seconds) {
        configManager.setLoginTimeoutSeconds(seconds);
    }

    public int getTimeout() {
        return configManager.getLoginTimeoutSeconds();
    }

    public CompletableFuture<Void> resetAccount(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            databaseManager.deleteUser(uuid);
            loggedInUsers.remove(uuid);
            joinTimes.remove(uuid);
            joinLocations.remove(uuid);
        });
    }

    public void shutdown() {
        databaseManager.close();
    }
}
