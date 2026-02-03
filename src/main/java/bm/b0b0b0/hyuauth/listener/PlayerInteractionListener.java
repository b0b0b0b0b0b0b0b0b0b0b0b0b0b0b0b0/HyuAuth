package bm.b0b0b0.hyuauth.listener;

import bm.b0b0b0.hyuauth.manager.AuthManager;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

public class PlayerInteractionListener {
    private final AuthManager authManager;

    public PlayerInteractionListener(AuthManager authManager) {
        this.authManager = authManager;
    }

    public void onPlayerChat(PlayerChatEvent event) {
        try {
            System.out.println("[HyuAuth] PlayerChatEvent: " + event.getClass().getName());
            PlayerRef sender = null;
            try {
                sender = event.getSender();
            } catch (Exception ignored) {
                try {
                    java.lang.reflect.Method getSenderMethod = event.getClass().getMethod("getSender");
                    getSenderMethod.setAccessible(true);
                    Object senderObj = getSenderMethod.invoke(event);
                    if (senderObj instanceof PlayerRef) {
                        sender = (PlayerRef) senderObj;
                    }
                } catch (Exception ignored2) {
                }
            }
            
            if (sender == null) {
                return;
            }
            
            UUID uuid = getUuidFromPlayerRef(sender);
            if (uuid == null) {
                return;
            }
            
            System.out.println("[HyuAuth] PlayerChatEvent: Player " + uuid + " isLoggedIn=" + authManager.isLoggedIn(uuid));
            
            if (!authManager.isLoggedIn(uuid)) {
                String message = null;
                try {
                    java.lang.reflect.Method getMessageMethod = event.getClass().getMethod("getMessage");
                    getMessageMethod.setAccessible(true);
                    Object msgObj = getMessageMethod.invoke(event);
                    if (msgObj != null) {
                        message = msgObj.toString();
                    }
                } catch (Exception ignored) {
                }
                
                if (message != null) {
                    String lowerMessage = message.toLowerCase().trim();
                    if (lowerMessage.startsWith("/login ") || lowerMessage.equals("/login") ||
                        lowerMessage.startsWith("/l ") || lowerMessage.equals("/l") ||
                        lowerMessage.startsWith("/register ") || lowerMessage.equals("/register") ||
                        lowerMessage.startsWith("/reg ") || lowerMessage.equals("/reg")) {
                        return;
                    }
                }
                
                if (event instanceof com.hypixel.hytale.event.ICancellable) {
                    ((com.hypixel.hytale.event.ICancellable) event).setCancelled(true);
                } else {
                    try {
                        java.lang.reflect.Method setCancelledMethod = event.getClass().getMethod("setCancelled", boolean.class);
                        setCancelledMethod.invoke(event, true);
                    } catch (Exception ignored) {
                    }
                }
                sender.sendMessage(Message.translation("hyuauth.messages.blocked").color("#FF0000").bold(true));
            }
        } catch (Exception ignored) {
        }
    }

    public void onPlayerInteract(PlayerInteractEvent event) {
        try {
            System.out.println("[HyuAuth] PlayerInteractEvent: " + event.getClass().getName());
            if (event instanceof PlayerEvent) {
                com.hypixel.hytale.server.core.entity.entities.Player player = ((PlayerEvent<?>)event).getPlayer();
                UUID uuid = getUuidFromPlayer(player);
                System.out.println("[HyuAuth] PlayerInteractEvent: Player " + uuid + " isLoggedIn=" + (uuid != null ? authManager.isLoggedIn(uuid) : "null"));
                if (uuid != null && !authManager.isLoggedIn(uuid)) {
                    System.out.println("[HyuAuth] PlayerInteractEvent: Blocking interaction for " + uuid);
                    if (event instanceof com.hypixel.hytale.event.ICancellable) {
                        ((com.hypixel.hytale.event.ICancellable) event).setCancelled(true);
                    } else {
                        try {
                            java.lang.reflect.Method setCancelledMethod = event.getClass().getMethod("setCancelled", boolean.class);
                            setCancelledMethod.invoke(event, true);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[HyuAuth] PlayerInteractEvent error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void onPlayerUseBlock(UseBlockEvent.Pre event) {
        try {
            System.out.println("[HyuAuth] UseBlockEvent.Pre: " + event.getClass().getName());
            com.hypixel.hytale.server.core.entity.InteractionContext context = event.getContext();
            if (context != null) {
                Ref<EntityStore> entityRef = context.getEntity();
                CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
                if (commandBuffer != null && entityRef != null) {
                    PlayerRef playerRef = commandBuffer.getComponent(entityRef, PlayerRef.getComponentType());
                    if (playerRef != null) {
                        UUID uuid = getUuidFromPlayerRef(playerRef);
                        System.out.println("[HyuAuth] UseBlockEvent.Pre: Player " + uuid + " isLoggedIn=" + (uuid != null ? authManager.isLoggedIn(uuid) : "null"));
                        if (uuid != null) {
                            boolean isLoggedIn = authManager.isLoggedIn(uuid);
                            if (!isLoggedIn) {
                                System.out.println("[HyuAuth] UseBlockEvent.Pre: Blocking block use for " + uuid);
                                if (event instanceof com.hypixel.hytale.component.system.ICancellableEcsEvent) {
                                    ((com.hypixel.hytale.component.system.ICancellableEcsEvent) event).setCancelled(true);
                                } else {
                                    try {
                                        event.setCancelled(true);
                                    } catch (Exception ignored) {
                                        try {
                                            java.lang.reflect.Method setCancelledMethod = event.getClass().getMethod("setCancelled", boolean.class);
                                            setCancelledMethod.invoke(event, true);
                                        } catch (Exception ignored2) {
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[HyuAuth] UseBlockEvent.Pre error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    private UUID getUuidFromPlayerRef(PlayerRef playerRef) {
        if (playerRef == null) {
            return null;
        }
        try {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null) {
                Store<EntityStore> store = ref.getStore();
                UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
                if (uuidComponent != null) {
                    return uuidComponent.getUuid();
                }
            }
        } catch (Exception ignored) {
        }
        try {
            return playerRef.getUuid();
        } catch (Exception ignored) {
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private UUID getUuidFromPlayer(com.hypixel.hytale.server.core.entity.entities.Player player) {
        if (player == null) {
            return null;
        }
        try {
            Ref<EntityStore> ref = player.getReference();
            if (ref != null) {
                Store<EntityStore> store = ref.getStore();
                UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
                if (uuidComponent != null) {
                    return uuidComponent.getUuid();
                }
            }
        } catch (Exception ignored) {
        }
        try {
            return player.getUuid();
        } catch (Exception ignored) {
        }
        return null;
    }
}
