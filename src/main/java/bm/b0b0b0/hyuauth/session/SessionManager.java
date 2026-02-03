package bm.b0b0b0.hyuauth.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SessionManager {
    private final Cache<String, PlayerSession> sessions;
    private final int sessionTimeoutMinutes;

    public SessionManager(int sessionTimeoutMinutes) {
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
        this.sessions = Caffeine.newBuilder()
                .expireAfterWrite(sessionTimeoutMinutes, TimeUnit.MINUTES)
                .build();
        System.out.println("[HyuAuth] SessionManager initialized with timeout: " + sessionTimeoutMinutes + " minutes");
    }

    public static class PlayerSession {
        public final String ipAddress;
        public final String username;
        public final UUID uuid;
        public final long lastLoginTime;

        public PlayerSession(String ipAddress, String username, UUID uuid) {
            this.ipAddress = ipAddress;
            this.username = username;
            this.uuid = uuid;
            this.lastLoginTime = System.currentTimeMillis();
        }
    }

    public void createSession(String ipAddress, String username, UUID uuid) {
        String sessionKey = ipAddress + ":" + username.toLowerCase();
        sessions.put(sessionKey, new PlayerSession(ipAddress, username, uuid));
        System.out.println("[HyuAuth] [SessionManager] Session created for " + username + " from " + ipAddress);
    }

    public PlayerSession getSession(String ipAddress, String username) {
        String sessionKey = ipAddress + ":" + username.toLowerCase();
        PlayerSession session = sessions.getIfPresent(sessionKey);
        if (session != null) {
            System.out.println("[HyuAuth] [SessionManager] Valid session found for " + username + " from " + ipAddress);
            return session;
        }
        System.out.println("[HyuAuth] [SessionManager] No valid session for " + username + " from " + ipAddress);
        return null;
    }

    public void removeSession(String ipAddress, String username) {
        String sessionKey = ipAddress + ":" + username.toLowerCase();
        sessions.invalidate(sessionKey);
        System.out.println("[HyuAuth] [SessionManager] Session removed for " + username + " from " + ipAddress);
    }

    public void removeSessionByUuid(UUID uuid) {
        sessions.asMap().entrySet().removeIf(entry -> entry.getValue().uuid.equals(uuid));
    }

    public void cleanupExpiredSessions() {
        sessions.cleanUp();
        System.out.println("[HyuAuth] [SessionManager] Cleaned up expired sessions");
    }
}
