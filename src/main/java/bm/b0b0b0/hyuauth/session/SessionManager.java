package bm.b0b0b0.hyuauth.session;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private final Map<String, PlayerSession> sessions = new ConcurrentHashMap<>();
    private final int sessionTimeoutMinutes;

    public SessionManager(int sessionTimeoutMinutes) {
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
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

        public boolean isValid(int timeoutMinutes) {
            long elapsedMinutes = (System.currentTimeMillis() - lastLoginTime) / (1000 * 60);
            return elapsedMinutes < timeoutMinutes;
        }
    }

    public void createSession(String ipAddress, String username, UUID uuid) {
        String sessionKey = ipAddress + ":" + username.toLowerCase();
        sessions.put(sessionKey, new PlayerSession(ipAddress, username, uuid));
        System.out.println("[HyuAuth] [SessionManager] Session created for " + username + " from " + ipAddress);
    }

    public PlayerSession getSession(String ipAddress, String username) {
        String sessionKey = ipAddress + ":" + username.toLowerCase();
        PlayerSession session = sessions.get(sessionKey);
        if (session != null && session.isValid(sessionTimeoutMinutes)) {
            return session;
        }
        if (session != null) {
            sessions.remove(sessionKey);
            System.out.println("[HyuAuth] [SessionManager] Session expired for " + username + " from " + ipAddress);
        }
        return null;
    }

    public void removeSession(String ipAddress, String username) {
        String sessionKey = ipAddress + ":" + username.toLowerCase();
        sessions.remove(sessionKey);
        System.out.println("[HyuAuth] [SessionManager] Session removed for " + username + " from " + ipAddress);
    }

    public void cleanupExpiredSessions() {
        int removed = 0;
        for (Map.Entry<String, PlayerSession> entry : sessions.entrySet()) {
            if (!entry.getValue().isValid(sessionTimeoutMinutes)) {
                sessions.remove(entry.getKey());
                removed++;
            }
        }
        if (removed > 0) {
            System.out.println("[HyuAuth] [SessionManager] Cleaned up " + removed + " expired sessions");
        }
    }

    public void removeSessionByUuid(UUID uuid) {
        sessions.entrySet().removeIf(entry -> entry.getValue().uuid.equals(uuid));
    }
}
