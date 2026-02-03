package bm.b0b0b0.hyuauth.events;

import bm.b0b0b0.hyuauth.HyuAuthPlugin;
import bm.b0b0b0.hyuauth.pages.LoginPage;
import bm.b0b0b0.hyuauth.pages.RegisterPage;
import bm.b0b0b0.hyuauth.services.AuthService;
import bm.b0b0b0.hyuauth.session.SessionManager;
import bm.b0b0b0.hyuauth.system.PlayerAuthMovementSystem;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerSetupConnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import javax.annotation.Nonnull;

public class PlayerAuthEvents {
    private final SessionManager sessionManager;
    private static PlayerAuthMovementSystem movementSystemInstance = null;

    public PlayerAuthEvents(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }
    
    public static void setMovementSystem(PlayerAuthMovementSystem system) {
        movementSystemInstance = system;
    }

    public void onPlayerReady(@Nonnull PlayerReadyEvent event) {
        System.out.println("[HyuAuth] [PlayerAuthEvents] onPlayerReady called");
        try {
            Player playerComponent = event.getPlayer();
            if (playerComponent == null) {
                System.out.println("[HyuAuth] [PlayerAuthEvents] ERROR: playerComponent is null!");
                return;
            }
            
            Ref<EntityStore> ref = event.getPlayerRef();
            if (ref == null) {
                System.out.println("[HyuAuth] [PlayerAuthEvents] ERROR: ref is null!");
                return;
            }
            
            World world = playerComponent.getWorld();
            if (world == null) {
                System.out.println("[HyuAuth] [PlayerAuthEvents] ERROR: world is null!");
                return;
            }
            
            try {
                if (movementSystemInstance != null) {
                    java.lang.reflect.Method getSystemRegistryMethod = world.getClass().getMethod("getSystemRegistry");
                    if (getSystemRegistryMethod != null) {
                        Object systemRegistry = getSystemRegistryMethod.invoke(world);
                        if (systemRegistry != null) {
                            java.lang.reflect.Method registerSystemMethod = systemRegistry.getClass().getMethod("registerSystem", Object.class);
                            if (registerSystemMethod != null) {
                                registerSystemMethod.invoke(systemRegistry, movementSystemInstance);
                                System.out.println("[HyuAuth] [PlayerAuthEvents] Movement system registered to world: " + world.getName());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("[HyuAuth] [PlayerAuthEvents] Could not register movement system (will use event-based check): " + e.getMessage());
            }
            
            Timer delayTimer = new Timer(true);
            delayTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    world.execute(() -> {
                        try {
                            Store<EntityStore> store = ref.getStore();
                            if (store == null) {
                                System.out.println("[HyuAuth] [PlayerAuthEvents] ERROR: store is null after delay!");
                                return;
                            }
                            
                            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                            if (playerRef == null) {
                                System.out.println("[HyuAuth] [PlayerAuthEvents] PlayerRef is null after delay, retrying in 2 seconds...");
                                Timer retryTimer = new Timer(true);
                                retryTimer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        world.execute(() -> {
                                            try {
                                                Store<EntityStore> retryStore = ref.getStore();
                                                PlayerRef retryPlayerRef = retryStore.getComponent(ref, PlayerRef.getComponentType());
                                                if (retryPlayerRef != null) {
                                                    @SuppressWarnings("deprecation")
                                                    UUID playerUuid = retryPlayerRef.getUuid();
                                                    String username = retryPlayerRef.getUsername();
                                                    String ipAddress = getPlayerIP(retryPlayerRef);
                                                    
                                                    System.out.println("[HyuAuth] [PlayerAuthEvents] Player ready (retry): " + username + " (" + playerUuid + ") from " + ipAddress);
                                                    
                                                    Boolean registered = AuthService.GetInstance().ParseFromDatabase(playerUuid);
                                                    System.out.println("[HyuAuth] [PlayerAuthEvents] Player registered: " + registered);
                                                    
                                                    openAuthPage(ref, retryStore, retryPlayerRef, registered.booleanValue(), ipAddress, username, playerUuid);
                                                } else {
                                                    System.out.println("[HyuAuth] [PlayerAuthEvents] PlayerRef is still null after retry!");
                                                }
                                            } catch (Exception e) {
                                                System.out.println("[HyuAuth] [PlayerAuthEvents] ERROR in retry: " + e.getMessage());
                                                e.printStackTrace();
                                            }
                                        });
                                    }
                                }, 2000L);
                                return;
                            }
                            
                            @SuppressWarnings("deprecation")
                            UUID playerUuid = playerRef.getUuid();
                            String username = playerRef.getUsername();
                            String ipAddress = getPlayerIP(playerRef);
                            
                            System.out.println("[HyuAuth] [PlayerAuthEvents] Player ready: " + username + " (" + playerUuid + ") from " + ipAddress);
                            
                            Boolean registered = AuthService.GetInstance().ParseFromDatabase(playerUuid);
                            System.out.println("[HyuAuth] [PlayerAuthEvents] Player registered: " + registered);
                            
                            openAuthPage(ref, store, playerRef, registered.booleanValue(), ipAddress, username, playerUuid);
                        } catch (Exception e) {
                            System.out.println("[HyuAuth] [PlayerAuthEvents] ERROR in delayed execution: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                }
            }, 2000L);
        } catch (Exception e) {
            System.out.println("[HyuAuth] [PlayerAuthEvents] ERROR in onPlayerReady: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openAuthPage(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PlayerRef playerRef, boolean isRegistered, String ipAddress, String username, UUID playerUuid) {
        try {
            Player playerComponent = store.getComponent(ref, Player.getComponentType());
            if (playerComponent == null) {
                System.out.println("[HyuAuth] [PlayerAuthEvents] ERROR: Cannot get player component for opening auth page");
                return;
            }
            
            if (!isRegistered) {
                System.out.println("[HyuAuth] [PlayerAuthEvents] Opening RegisterPage");
                try {
                    PlayerAuthMovementSystem.markPageOpen(playerUuid);
                    playerComponent.getPageManager().openCustomPage(ref, store, new RegisterPage(playerRef, sessionManager));
                    System.out.println("[HyuAuth] [PlayerAuthEvents] RegisterPage opened successfully");
                } catch (Exception e) {
                    System.out.println("[HyuAuth] [PlayerAuthEvents] ERROR opening RegisterPage: " + e.getMessage());
                    e.printStackTrace();
                }
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
                    System.out.println("[HyuAuth] [PlayerAuthEvents] Auto-authentication completed");
                } else {
                    System.out.println("[HyuAuth] [PlayerAuthEvents] No valid session, opening LoginPage");
                    try {
                        PlayerAuthMovementSystem.markPageOpen(playerUuid);
                        playerComponent.getPageManager().openCustomPage(ref, store, new LoginPage(playerRef, sessionManager));
                        System.out.println("[HyuAuth] [PlayerAuthEvents] LoginPage opened successfully");
                    } catch (Exception e) {
                        System.out.println("[HyuAuth] [PlayerAuthEvents] ERROR opening LoginPage: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[HyuAuth] [PlayerAuthEvents] ERROR in openAuthPage: " + e.getMessage());
            e.printStackTrace();
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

    public void onPlayerSetupConnect(@Nonnull PlayerSetupConnectEvent event) {
        try {
            String username = event.getUsername();
            UUID uuid = event.getUuid();
            System.out.println("[HyuAuth] [PlayerAuthEvents] Player setup connect: " + username + " (" + uuid + ")");
        } catch (Exception e) {
            System.out.println("[HyuAuth] [PlayerAuthEvents] Error in onPlayerSetupConnect: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void onPlayerConnect(@Nonnull PlayerConnectEvent event) {
        try {
            PlayerRef playerRef = event.getPlayerRef();
            if (playerRef != null) {
                @SuppressWarnings("deprecation")
                UUID playerUuid = playerRef.getUuid();
                String username = playerRef.getUsername();
                System.out.println("[HyuAuth] [PlayerAuthEvents] Player connect: " + username + " (" + playerUuid + ")");
            }
        } catch (Exception e) {
            System.out.println("[HyuAuth] [PlayerAuthEvents] Error in onPlayerConnect: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void onAddPlayerToWorld(@Nonnull AddPlayerToWorldEvent event) {
        try {
            Holder<EntityStore> holder = event.getHolder();
            if (holder != null) {
                try {
                    PlayerRef playerRef = holder.getComponent(PlayerRef.getComponentType());
                    if (playerRef != null) {
                        @SuppressWarnings("deprecation")
                        UUID playerUuid = playerRef.getUuid();
                        String username = playerRef.getUsername();
                        System.out.println("[HyuAuth] [PlayerAuthEvents] Player added to world: " + username + " (" + playerUuid + ")");
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            System.out.println("[HyuAuth] [PlayerAuthEvents] Error in onAddPlayerToWorld: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void onPlayerDisconnect(@Nonnull PlayerDisconnectEvent event) {
        try {
            PlayerRef playerRef = event.getPlayerRef();
            if (playerRef != null) {
                @SuppressWarnings("deprecation")
                UUID playerUuid = playerRef.getUuid();
                String username = playerRef.getUsername();
                String ipAddress = getPlayerIP(playerRef);
                
                Boolean isAuthenticated = AuthService.GetInstance().GetPlayer(playerUuid);
                if (isAuthenticated != null && isAuthenticated) {
                    System.out.println("[HyuAuth] [PlayerAuthEvents] Player " + username + " disconnected, saving session");
                    sessionManager.createSession(ipAddress, username, playerUuid);
                }
                PlayerAuthMovementSystem.markPageClosed(playerUuid);
            }
        } catch (Exception e) {
            System.out.println("[HyuAuth] [PlayerAuthEvents] Error in onPlayerDisconnect: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void onPlayerQuit(@Nonnull DrainPlayerFromWorldEvent event) {
        try {
            Holder<EntityStore> holder = event.getHolder();
            if (holder != null) {
                try {
                    PlayerRef playerRef = holder.getComponent(PlayerRef.getComponentType());
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
                        PlayerAuthMovementSystem.markPageClosed(playerUuid);
                        return;
                    }
                } catch (Exception ignored) {
                }
            }
            
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
                PlayerAuthMovementSystem.markPageClosed(playerUuid);
            } else {
                System.out.println("[HyuAuth] [PlayerAuthEvents] Could not get PlayerRef from DrainPlayerFromWorldEvent");
            }
        } catch (Exception e) {
            System.out.println("[HyuAuth] [PlayerAuthEvents] Error in onPlayerQuit: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
