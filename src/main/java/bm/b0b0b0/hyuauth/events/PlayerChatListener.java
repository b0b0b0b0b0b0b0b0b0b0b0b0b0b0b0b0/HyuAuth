package bm.b0b0b0.hyuauth.events;

import bm.b0b0b0.hyuauth.services.AuthService;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.UUID;
import javax.annotation.Nonnull;

public class PlayerChatListener {
    
    public void onPlayerChat(@Nonnull Object event) {
        System.out.println("[HyuAuth] [PlayerChatListener] Chat event received: " + event.getClass().getName());
        try {
            PlayerRef playerRef = null;
            
            if (event instanceof PlayerChatEvent) {
                PlayerChatEvent chatEvent = (PlayerChatEvent) event;
                playerRef = chatEvent.getSender();
                
                if (playerRef != null) {
                    @SuppressWarnings("deprecation")
                    UUID playerUuid = playerRef.getUuid();
                    String username = playerRef.getUsername();
                    
                    System.out.println("[HyuAuth] [PlayerChatListener] Player " + username + " (" + playerUuid + ") trying to chat");
                    
                    Boolean isAuthenticated = AuthService.GetInstance().GetPlayer(playerUuid);
                    System.out.println("[HyuAuth] [PlayerChatListener] Is authenticated: " + isAuthenticated);
                    
                    if (isAuthenticated == null || !isAuthenticated) {
                        System.out.println("[HyuAuth] [PlayerChatListener] Blocking chat for unauthenticated player");
                        chatEvent.setCancelled(true);
                        System.out.println("[HyuAuth] [PlayerChatListener] Chat event cancelled successfully");
                    }
                    return;
                }
            }
            
            try {
                java.lang.reflect.Method getSenderMethod = event.getClass().getMethod("getSender");
                getSenderMethod.setAccessible(true);
                Object playerObj = getSenderMethod.invoke(event);
                if (playerObj instanceof PlayerRef) {
                    playerRef = (PlayerRef) playerObj;
                }
            } catch (Exception ignored) {
            }
            
            if (playerRef == null) {
                try {
                    java.lang.reflect.Method getPlayerMethod = event.getClass().getMethod("getPlayer");
                    getPlayerMethod.setAccessible(true);
                    Object playerObj = getPlayerMethod.invoke(event);
                    if (playerObj instanceof PlayerRef) {
                        playerRef = (PlayerRef) playerObj;
                    }
                } catch (Exception ignored) {
                }
            }
            
            if (playerRef == null) {
                try {
                    java.lang.reflect.Method getPlayerRefMethod = event.getClass().getMethod("getPlayerRef");
                    getPlayerRefMethod.setAccessible(true);
                    Object playerObj = getPlayerRefMethod.invoke(event);
                    if (playerObj instanceof PlayerRef) {
                        playerRef = (PlayerRef) playerObj;
                    }
                } catch (Exception ignored) {
                }
            }
            
            if (playerRef == null) {
                System.out.println("[HyuAuth] [PlayerChatListener] Could not get PlayerRef from chat event");
                return;
            }
            
            @SuppressWarnings("deprecation")
            UUID playerUuid = playerRef.getUuid();
            String username = playerRef.getUsername();
            
            System.out.println("[HyuAuth] [PlayerChatListener] Player " + username + " (" + playerUuid + ") trying to chat");
            
            Boolean isAuthenticated = AuthService.GetInstance().GetPlayer(playerUuid);
            System.out.println("[HyuAuth] [PlayerChatListener] Is authenticated: " + isAuthenticated);
            
            if (isAuthenticated == null || !isAuthenticated) {
                System.out.println("[HyuAuth] [PlayerChatListener] Blocking chat for unauthenticated player");
                try {
                    java.lang.reflect.Method setCancelledMethod = event.getClass().getMethod("setCancelled", boolean.class);
                    setCancelledMethod.setAccessible(true);
                    setCancelledMethod.invoke(event, true);
                    System.out.println("[HyuAuth] [PlayerChatListener] Chat event cancelled successfully");
                } catch (Exception e) {
                    System.out.println("[HyuAuth] [PlayerChatListener] Could not cancel chat event: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.out.println("[HyuAuth] [PlayerChatListener] Error checking chat: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
