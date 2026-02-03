package bm.b0b0b0.hyuauth.system;

import bm.b0b0b0.hyuauth.events.PlayerAuthEvents;
import bm.b0b0b0.hyuauth.pages.LoginPage;
import bm.b0b0b0.hyuauth.pages.RegisterPage;
import bm.b0b0b0.hyuauth.services.AuthService;
import bm.b0b0b0.hyuauth.session.SessionManager;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import javax.annotation.Nonnull;

public class PlayerAuthMovementSystem extends EntityTickingSystem<EntityStore> {
    private static final com.hypixel.hytale.component.ComponentType<EntityStore, TransformComponent> TRANSFORM_COMPONENT_TYPE = TransformComponent.getComponentType();
    private static final Query<EntityStore> QUERY = Query.and(PlayerRef.getComponentType(), TRANSFORM_COMPONENT_TYPE);
    
    private final SessionManager sessionManager;
    private final Map<UUID, Long> lastCheckTime = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> openPages = new ConcurrentHashMap<>();
    private static final long CHECK_INTERVAL_MS = 2000L;

    public PlayerAuthMovementSystem(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> archetypeChunk, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> entityRef = archetypeChunk.getReferenceTo(index);
        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        
        com.hypixel.hytale.server.core.entity.UUIDComponent uuidComponent = store.getComponent(entityRef, com.hypixel.hytale.server.core.entity.UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            return;
        }
        
        @SuppressWarnings("deprecation")
        UUID uuid = uuidComponent.getUuid();
        
        Boolean isAuthenticated = AuthService.GetInstance().GetPlayer(uuid);
        if (isAuthenticated != null && isAuthenticated) {
            lastCheckTime.remove(uuid);
            openPages.remove(uuid);
            return;
        }
        
        Long lastCheck = lastCheckTime.get(uuid);
        long currentTime = System.currentTimeMillis();
        if (lastCheck != null && (currentTime - lastCheck) < CHECK_INTERVAL_MS) {
            return;
        }
        lastCheckTime.put(uuid, currentTime);
        
        Boolean hasOpenPage = openPages.get(uuid);
        if (hasOpenPage != null && hasOpenPage) {
            return;
        }
        
        try {
            TransformComponent transform = store.getComponent(entityRef, TRANSFORM_COMPONENT_TYPE);
            if (transform == null) {
                return;
            }
            
            Player playerComponent = store.getComponent(entityRef, Player.getComponentType());
            if (playerComponent == null) {
                return;
            }
            
            Boolean registered = AuthService.GetInstance().ParseFromDatabase(uuid);
            String username = playerRef.getUsername();
            String ipAddress = getPlayerIP(playerRef);
            
            if (!registered.booleanValue()) {
                openPages.put(uuid, true);
                playerComponent.getPageManager().openCustomPage(entityRef, store, new RegisterPage(playerRef, sessionManager));
                System.out.println("[HyuAuth] [PlayerAuthMovementSystem] Opened RegisterPage for " + username + " on movement");
            } else {
                SessionManager.PlayerSession session = sessionManager.getSession(ipAddress, username);
                if (session == null || !session.uuid.equals(uuid)) {
                    openPages.put(uuid, true);
                    playerComponent.getPageManager().openCustomPage(entityRef, store, new LoginPage(playerRef, sessionManager));
                    System.out.println("[HyuAuth] [PlayerAuthMovementSystem] Opened LoginPage for " + username + " on movement");
                }
            }
        } catch (Exception e) {
            System.out.println("[HyuAuth] [PlayerAuthMovementSystem] Error checking player: " + e.getMessage());
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
            System.out.println("[HyuAuth] [PlayerAuthMovementSystem] Error getting IP: " + e.getMessage());
        }
        return "unknown";
    }
    
    public static void markPageOpen(UUID uuid) {
        openPages.put(uuid, true);
    }
    
    public static void markPageClosed(UUID uuid) {
        openPages.remove(uuid);
    }
}
