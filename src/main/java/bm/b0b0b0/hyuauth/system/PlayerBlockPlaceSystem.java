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
        System.out.println("[HyuAuth] [EVENT-LOG] PlayerBlockPlaceSystem.handle() CALLED | index=" + index + " | event=" + event.getClass().getName());
        try {
            UUIDComponent component = archetypeChunk.getComponent(index, UUIDComponent.getComponentType());
            System.out.println("[HyuAuth] [EVENT-LOG] PlayerBlockPlaceSystem | UUIDComponent=" + (component != null ? "found" : "null"));
            if (component != null) {
                @SuppressWarnings("deprecation")
                UUID playerUuid = component.getUuid();
                System.out.println("[HyuAuth] [EVENT-LOG] PlaceBlockEvent (PLACE) | Player: " + playerUuid + " | Action: BLOCK_PLACE");
                boolean isLoggedIn = authManager.isLoggedIn(playerUuid);
                if (!isLoggedIn) {
                    System.out.println("[HyuAuth] [EVENT-LOG] PlaceBlockEvent (PLACE) | BLOCKED for " + playerUuid);
                    event.setCancelled(true);
                }
            } else {
                System.out.println("[HyuAuth] [EVENT-LOG] PlaceBlockEvent (PLACE) | Player: UNKNOWN | No UUIDComponent at index " + index);
            }
        } catch (Exception e) {
            System.out.println("[HyuAuth] [EVENT-LOG] PlaceBlockEvent (PLACE) | ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
