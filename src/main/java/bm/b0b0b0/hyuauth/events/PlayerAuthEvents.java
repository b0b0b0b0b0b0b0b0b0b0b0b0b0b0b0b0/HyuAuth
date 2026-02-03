package bm.b0b0b0.hyuauth.events;

import bm.b0b0b0.hyuauth.pages.LoginPage;
import bm.b0b0b0.hyuauth.pages.RegisterPage;
import bm.b0b0b0.hyuauth.services.AuthService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;

public class PlayerAuthEvents {
    public void onPlayerReady(@Nonnull PlayerReadyEvent event) {
        System.out.println("[HyuAuth] [PlayerAuthEvents] onPlayerReady called");
        Player playerComponent = event.getPlayer();
        Ref<EntityStore> ref = event.getPlayerRef();
        Store<EntityStore> store = ref.getStore();
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null) {
            @SuppressWarnings("deprecation")
            UUID playerUuid = playerRef.getUuid();
            System.out.println("[HyuAuth] [PlayerAuthEvents] Player ready: " + playerRef.getUsername() + " (" + playerUuid + ")");
            
            Boolean registered = AuthService.GetInstance().ParseFromDatabase(playerUuid);
            System.out.println("[HyuAuth] [PlayerAuthEvents] Player registered: " + registered);
            
            if (!registered.booleanValue()) {
                System.out.println("[HyuAuth] [PlayerAuthEvents] Opening RegisterPage");
                playerComponent.getPageManager().openCustomPage(ref, store, new RegisterPage(playerRef));
            } else {
                System.out.println("[HyuAuth] [PlayerAuthEvents] Opening LoginPage");
                playerComponent.getPageManager().openCustomPage(ref, store, new LoginPage(playerRef));
            }
        } else {
            System.out.println("[HyuAuth] [PlayerAuthEvents] PlayerRef is null!");
        }
    }
}
