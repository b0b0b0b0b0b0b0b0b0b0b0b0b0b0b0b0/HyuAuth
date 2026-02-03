package bm.b0b0b0.hyuauth.events;

import bm.b0b0b0.hyuauth.pages.LoginPage;
import bm.b0b0b0.hyuauth.pages.RegisterPage;
import bm.b0b0b0.hyuauth.services.AuthService;
import bm.b0b0b0.hyuauth.session.SessionManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;

public class PlayerAuthEvents {
    private final SessionManager sessionManager;

    public PlayerAuthEvents(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void onPlayerReady(@Nonnull PlayerReadyEvent event) {
        System.out.println("[HyuAuth] [PlayerAuthEvents] onPlayerReady called");
        Player playerComponent = event.getPlayer();
        Ref<EntityStore> ref = event.getPlayerRef();
        Store<EntityStore> store = ref.getStore();
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null) {
            @SuppressWarnings("deprecation")
            UUID playerUuid = playerRef.getUuid();
            String username = playerRef.getUsername();
            String ipAddress = getPlayerIP(playerRef);
            
            System.out.println("[HyuAuth] [PlayerAuthEvents] Player ready: " + username + " (" + playerUuid + ") from " + ipAddress);
            
            Boolean registered = AuthService.GetInstance().ParseFromDatabase(playerUuid);
            System.out.println("[HyuAuth] [PlayerAuthEvents] Player registered: " + registered);
            
            if (!registered.booleanValue()) {
                System.out.println("[HyuAuth] [PlayerAuthEvents] Opening RegisterPage");
                playerComponent.getPageManager().openCustomPage(ref, store, new RegisterPage(playerRef, sessionManager));
            } else {
                SessionManager.PlayerSession session = sessionManager.getSession(ipAddress, username);
                if (session != null && session.uuid.equals(playerUuid)) {
                    System.out.println("[HyuAuth] [PlayerAuthEvents] Valid session found, auto-authenticating");
                    AuthService.GetInstance()._authInstances.put(playerUuid, true);
                    sessionManager.createSession(ipAddress, username, playerUuid);
                    Message sessionMessage = Message.raw("Сессия активна! Авторизация прошла автоматически.")
                            .color("#00FF00")
                            .bold(true);
                    playerComponent.sendMessage(sessionMessage);
                } else {
                    System.out.println("[HyuAuth] [PlayerAuthEvents] No valid session, opening LoginPage");
                    playerComponent.getPageManager().openCustomPage(ref, store, new LoginPage(playerRef, sessionManager));
                }
            }
        } else {
            System.out.println("[HyuAuth] [PlayerAuthEvents] PlayerRef is null!");
        }
    }

    private String getPlayerIP(@Nonnull PlayerRef playerRef) {
        try {
            com.hypixel.hytale.server.core.io.PacketHandler packetHandler = playerRef.getPacketHandler();
            if (packetHandler != null) {
                try {
                    java.lang.reflect.Method getConnectionMethod = packetHandler.getClass().getMethod("getConnection");
                    getConnectionMethod.setAccessible(true);
                    Object connection = getConnectionMethod.invoke(packetHandler);
                    if (connection != null) {
                        try {
                            java.lang.reflect.Method getRemoteAddressMethod = connection.getClass().getMethod("getRemoteAddress");
                            getRemoteAddressMethod.setAccessible(true);
                            Object address = getRemoteAddressMethod.invoke(connection);
                            if (address != null) {
                                String addressStr = address.toString();
                                if (addressStr.contains("/")) {
                                    return addressStr.split("/")[1].split(":")[0];
                                }
                                return addressStr.split(":")[0];
                            }
                        } catch (Exception ignored) {
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            System.out.println("[HyuAuth] [PlayerAuthEvents] Error getting IP: " + e.getMessage());
        }
        return "unknown";
    }

    public void onPlayerQuit(@Nonnull DrainPlayerFromWorldEvent event) {
        try {
            Ref<EntityStore> ref = null;
            PlayerRef playerRef = null;
            
            try {
                java.lang.reflect.Method[] methods = event.getClass().getMethods();
                for (java.lang.reflect.Method method : methods) {
                    String methodName = method.getName();
                    if ((methodName.equals("getEntity") || methodName.equals("getPlayerRef") || methodName.equals("getRef")) 
                        && method.getParameterCount() == 0) {
                        method.setAccessible(true);
                        Object result = method.invoke(event);
                        if (result instanceof Ref) {
                            ref = (Ref<EntityStore>) result;
                            break;
                        } else if (result instanceof PlayerRef) {
                            playerRef = (PlayerRef) result;
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            
            if (ref != null) {
                Store<EntityStore> store = ref.getStore();
                if (playerRef == null) {
                    playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                }
            }
            
            if (playerRef == null) {
                try {
                    java.lang.reflect.Field[] fields = event.getClass().getDeclaredFields();
                    for (java.lang.reflect.Field field : fields) {
                        if (field.getType().equals(PlayerRef.class) || field.getType().equals(Ref.class)) {
                            field.setAccessible(true);
                            Object value = field.get(event);
                            if (value instanceof PlayerRef) {
                                playerRef = (PlayerRef) value;
                                break;
                            } else if (value instanceof Ref) {
                                ref = (Ref<EntityStore>) value;
                                Store<EntityStore> store = ref.getStore();
                                playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            
            if (playerRef != null) {
                @SuppressWarnings("deprecation")
                UUID playerUuid = playerRef.getUuid();
                String username = playerRef.getUsername();
                String ipAddress = getPlayerIP(playerRef);
                
                Boolean isAuthenticated = AuthService.GetInstance().GetPlayer(playerUuid);
                if (isAuthenticated != null && isAuthenticated) {
                    System.out.println("[HyuAuth] [PlayerAuthEvents] Player " + username + " quit, saving session");
                    sessionManager.createSession(ipAddress, username, playerUuid);
                }
            } else {
                System.out.println("[HyuAuth] [PlayerAuthEvents] Could not get PlayerRef from DrainPlayerFromWorldEvent");
            }
        } catch (Exception e) {
            System.out.println("[HyuAuth] [PlayerAuthEvents] Error in onPlayerQuit: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
