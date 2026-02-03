package bm.b0b0b0.hyuauth.system;

import bm.b0b0b0.hyuauth.manager.AuthManager;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class PlayerMovementSystem extends EntityTickingSystem<EntityStore> {
    private static final com.hypixel.hytale.component.ComponentType<EntityStore, TransformComponent> TRANSFORM_COMPONENT_TYPE = TransformComponent.getComponentType();
    private static final Query<EntityStore> QUERY = Query.and(PlayerRef.getComponentType(), TRANSFORM_COMPONENT_TYPE);
    
    private final AuthManager authManager;
    private final Map<UUID, Long> lastTeleportTime = new ConcurrentHashMap<>();

    public PlayerMovementSystem(AuthManager authManager) {
        this.authManager = authManager;
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
        if (!authManager.isLoggedIn(uuid)) {
            TransformComponent transform = store.getComponent(entityRef, TRANSFORM_COMPONENT_TYPE);
            if (transform != null) {
                com.hypixel.hytale.math.vector.Vector3d currentPos = transform.getPosition();
                com.hypixel.hytale.math.vector.Vector3d savedPos = authManager.getJoinLocation(uuid);
                if (savedPos != null) {
                    double distance = currentPos.distanceTo(savedPos);
                    if (distance > 0.5D) {
                        Long lastTime = lastTeleportTime.get(uuid);
                        long currentTime = System.currentTimeMillis();
                        if (lastTime == null || (currentTime - lastTime) > 100L) {
                            String cmd = String.format("tp %s %f %f %f", playerRef.getUsername(), savedPos.x, savedPos.y, savedPos.z);
                            CommandManager.get().handleCommand(ConsoleSender.INSTANCE, cmd);
                            lastTeleportTime.put(uuid, currentTime);
                        }
                    }
                } else {
                    authManager.markJoinLocation(uuid, currentPos.clone());
                }
            }
        } else {
            lastTeleportTime.remove(uuid);
        }
    }
}
