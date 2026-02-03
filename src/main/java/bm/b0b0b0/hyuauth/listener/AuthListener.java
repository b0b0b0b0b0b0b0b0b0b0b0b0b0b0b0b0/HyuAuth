package bm.b0b0b0.hyuauth.listener;

import bm.b0b0b0.hyuauth.manager.AuthManager;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class AuthListener {
    private final AuthManager authManager;
    private final Set<World> worlds = new HashSet<>();

    public AuthListener(AuthManager authManager) {
        this.authManager = authManager;
    }

    public void onPlayerJoin(AddPlayerToWorldEvent event) {
        synchronized (this.worlds) {
            this.worlds.add(event.getWorld());
        }
        System.out.println("[HyuAuth] Player joined world: " + event.getWorld().getName());
    }

    @SuppressWarnings("unused")
    public void onPlayerQuit(DrainPlayerFromWorldEvent event) {
    }

    public Set<World> getWorlds() {
        return this.worlds;
    }

    @SuppressWarnings("deprecation")
    public void checkAuthLoop() {
        try {
            ArrayList<World> worldList;
            synchronized (this.worlds) {
                worldList = new ArrayList<>(this.worlds);
            }

            if (worldList.isEmpty()) {
                List<PlayerRef> players = Universe.get().getPlayers();
                if (!players.isEmpty()) {
                    Set<World> foundWorlds = new HashSet<>();
                    for (PlayerRef playerRef : players) {
                        try {
                            UUID worldUuid = playerRef.getWorldUuid();
                            if (worldUuid != null) {
                                World world = Universe.get().getWorld(worldUuid);
                                if (world != null) {
                                    foundWorlds.add(world);
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    synchronized (this.worlds) {
                        this.worlds.addAll(foundWorlds);
                        worldList = new ArrayList<>(this.worlds);
                    }
                    if (!foundWorlds.isEmpty()) {
                        System.out.println("[HyuAuth] Found " + foundWorlds.size() + " world(s) from existing players");
                    }
                }
            }

            if (System.currentTimeMillis() % 10000L < 500L) {
                int totalPlayers = 0;
                for (World w : worldList) {
                    try {
                        totalPlayers += w.getPlayers().size();
                    } catch (Exception ignored) {
                    }
                }
                System.out.println("[HyuAuth] Heartbeat - Monitoring " + worldList.size() + " world(s), " + totalPlayers + " player(s)");
            }

            Iterator<World> worldIterator = worldList.iterator();
            while (worldIterator.hasNext()) {
                World world = worldIterator.next();
                List<Player> players = world.getPlayers();
                Iterator<Player> playerIterator = players.iterator();

                while (playerIterator.hasNext()) {
                    Player player = playerIterator.next();
                    @SuppressWarnings("deprecation")
                    UUID uuid = player.getUuid();
                    if (!authManager.isLoggedIn(uuid)) {
                        authManager.markJoin(uuid);

                        try {
                            Vector3d currentPos = CompletableFuture.supplyAsync(() -> {
                                try {
                                    return player.getTransformComponent().getPosition();
                                } catch (Exception e) {
                                    return null;
                                }
                            }, (Executor) world).get();
                            
                            if (currentPos == null) {
                                continue;
                            }
                            
                            Vector3d savedPos = authManager.getJoinLocation(uuid);
                            if (savedPos == null) {
                                authManager.markJoinLocation(uuid, currentPos.clone());
                                System.out.println("[HyuAuth] Saved initial location for " + player.getDisplayName() + ": " + currentPos);
                            } else {
                                double distance = currentPos.distanceTo(savedPos);
                                if (distance > 0.5D) {
                                    String cmd = String.format("tp %s %f %f %f", player.getDisplayName(), savedPos.x, savedPos.y, savedPos.z);
                                    CommandManager.get().handleCommand(ConsoleSender.INSTANCE, cmd);
                                    System.out.println("[HyuAuth] Teleported " + player.getDisplayName() + " back (distance: " + String.format("%.2f", distance) + ")");
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("[HyuAuth] Error getting player position: " + e.getMessage());
                        }

                        boolean shouldKick = authManager.shouldKick(uuid);
                        if (shouldKick) {
                            System.out.println("[HyuAuth] Kicking player " + player.getDisplayName() + " (timeout)");
                            if (player.getPlayerConnection() != null) {
                                PacketHandler connection = player.getPlayerConnection();
                                connection.disconnect(Message.translation("hyuauth.messages.timeout_kick").getAnsiMessage());
                                authManager.markQuit(uuid);
                            } else {
                                System.out.println("[HyuAuth] Warning: Cannot kick " + player.getDisplayName() + " (no connection)");
                            }
                        } else if (System.currentTimeMillis() % 2000L < 500L) {
                            player.sendMessage(Message.translation("hyuauth.messages.blocked").color("#FF0000").bold(true));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[HyuAuth] Error in authentication loop: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
