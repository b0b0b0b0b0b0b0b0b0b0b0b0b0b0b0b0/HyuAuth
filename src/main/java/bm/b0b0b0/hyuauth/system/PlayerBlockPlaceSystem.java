package bm.b0b0b0.hyuauth.system;

import bm.b0b0b0.hyuauth.manager.AuthManager;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

public class PlayerBlockPlaceSystem extends com.hypixel.hytale.component.system.EntityEventSystem<EntityStore, PlaceBlockEvent> {
    private final AuthManager authManager;

    public PlayerBlockPlaceSystem(AuthManager authManager) {
        super(PlaceBlockEvent.class);
        this.authManager = authManager;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return com.hypixel.hytale.component.Archetype.empty();
    }

    @Override
    public void handle(int index, ArchetypeChunk<EntityStore> archetypeChunk, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer, PlaceBlockEvent event) {
        try {
            UUIDComponent component = archetypeChunk.getComponent(index, UUIDComponent.getComponentType());
            if (component != null) {
                UUID playerUuid = component.getUuid();
                boolean isLoggedIn = authManager.isLoggedIn(playerUuid);
                if (!isLoggedIn) {
                    System.out.println("[HyuAuth] BlockPlaceSystem: Blocking block place for " + playerUuid);
                    event.setCancelled(true);
                }
            }
        } catch (Exception e) {
            System.out.println("[HyuAuth] BlockPlaceSystem error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
