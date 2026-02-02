package bm.b0b0b0.hyuauth.system;

import bm.b0b0b0.hyuauth.manager.AuthManager;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

public class PlayerDamageSystem extends DamageEventSystem {
    private final AuthManager authManager;

    public PlayerDamageSystem(AuthManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void handle(int index, ArchetypeChunk<EntityStore> archetypeChunk, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer, Damage damage) {
        PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        
        com.hypixel.hytale.server.core.entity.UUIDComponent uuidComponent = archetypeChunk.getComponent(index, com.hypixel.hytale.server.core.entity.UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            return;
        }
        
        UUID playerUuid = uuidComponent.getUuid();
        if (!authManager.isLoggedIn(playerUuid)) {
            damage.setAmount(0.0f);
        }
    }
}
