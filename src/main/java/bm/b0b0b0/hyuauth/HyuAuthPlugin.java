package bm.b0b0b0.hyuauth;

import bm.b0b0b0.hyuauth.command.AuthCommands;
import bm.b0b0b0.hyuauth.config.ConfigManager;
import bm.b0b0b0.hyuauth.listener.AuthListener;
import bm.b0b0b0.hyuauth.listener.PlayerInteractionListener;
import bm.b0b0b0.hyuauth.manager.AuthManager;
import bm.b0b0b0.hyuauth.system.PlayerBlockBreakSystem;
import bm.b0b0b0.hyuauth.system.PlayerBlockPlaceSystem;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HyuAuthPlugin extends JavaPlugin {
    private AuthManager authManager;
    private AuthListener authListener;
    private PlayerInteractionListener playerInteractionListener;
    private ConfigManager configManager;
    private ScheduledExecutorService scheduler;

    public HyuAuthPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        getLogger().atInfo().log("================================================");
        getLogger().atInfo().log("[HyuAuth] Initializing authentication plugin...");
        getLogger().atInfo().log("================================================");
        
        getLogger().atInfo().log("[HyuAuth] Loading configuration...");
        configManager = new ConfigManager(getDataDirectory());
        getLogger().atInfo().log("[HyuAuth] Configuration loaded successfully");
        
        getLogger().atInfo().log("[HyuAuth] Initializing database manager...");
        authManager = new AuthManager(getDataDirectory(), configManager);
        getLogger().atInfo().log("[HyuAuth] Database manager initialized");
        
        getLogger().atInfo().log("[HyuAuth] Initializing auth listener...");
        authListener = new AuthListener(authManager);
        getLogger().atInfo().log("[HyuAuth] Auth listener initialized");
        
        getLogger().atInfo().log("[HyuAuth] Initializing player interaction listener...");
        playerInteractionListener = new PlayerInteractionListener(authManager);
        getLogger().atInfo().log("[HyuAuth] Player interaction listener initialized");
        
        getLogger().atInfo().log("[HyuAuth] Registering commands...");
        getCommandRegistry().registerCommand(new AuthCommands.LoginCommand(authManager));
        getCommandRegistry().registerCommand(new AuthCommands.LoginShortCommand(authManager));
        getCommandRegistry().registerCommand(new AuthCommands.RegisterCommand(authManager));
        getCommandRegistry().registerCommand(new AuthCommands.RegisterShortCommand(authManager));
        getCommandRegistry().registerCommand(new AuthCommands.LogoutCommand(authManager));
        getCommandRegistry().registerCommand(new AuthCommands.AuthResetCommand(authManager));
        getCommandRegistry().registerCommand(new AuthCommands.AuthConfigCommand(authManager));
        getLogger().atInfo().log("[HyuAuth] Commands registered: /login, /l, /register, /reg, /logout, /authreset, /authconfig");
        
        getLogger().atInfo().log("[HyuAuth] Registering event listeners...");
        getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, authListener::onPlayerJoin);
        getEventRegistry().registerGlobal(DrainPlayerFromWorldEvent.class, authListener::onPlayerQuit);
        getEventRegistry().registerGlobal(PlayerChatEvent.class, playerInteractionListener::onPlayerChat);
        getEventRegistry().registerGlobal(PlayerInteractEvent.class, playerInteractionListener::onPlayerInteract);
        getEventRegistry().registerGlobal(UseBlockEvent.Pre.class, playerInteractionListener::onPlayerUseBlock);
        getLogger().atInfo().log("[HyuAuth] Event listeners registered: AddPlayerToWorldEvent, DrainPlayerFromWorldEvent, PlayerChatEvent, PlayerInteractEvent, UseBlockEvent.Pre");
        
        getLogger().atInfo().log("[HyuAuth] Setup completed successfully!");
        getLogger().atInfo().log("================================================");
    }

    @Override
    protected void start() {
        getLogger().atInfo().log("================================================");
        getLogger().atInfo().log("[HyuAuth] Starting authentication plugin...");
        getLogger().atInfo().log("================================================");
        
        getLogger().atInfo().log("[HyuAuth] Registering ECS systems...");
        getEntityStoreRegistry().registerSystem(new PlayerBlockPlaceSystem(authManager));
        getEntityStoreRegistry().registerSystem(new PlayerBlockBreakSystem(authManager));
        getLogger().atInfo().log("[HyuAuth] ECS systems registered: PlayerBlockPlaceSystem, PlayerBlockBreakSystem");
        
        getLogger().atInfo().log("[HyuAuth] Starting scheduled authentication loop...");
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(authListener::checkAuthLoop, 100L, 500L, TimeUnit.MILLISECONDS);
        getLogger().atInfo().log("[HyuAuth] Authentication loop started (interval: 500ms)");
        
        getLogger().atInfo().log("================================================");
        getLogger().atInfo().log("[HyuAuth] Plugin started successfully!");
        getLogger().atInfo().log("[HyuAuth] Players will be blocked until authentication");
        getLogger().atInfo().log("[HyuAuth] Use /login <password> or /register <password>");
        getLogger().atInfo().log("================================================");
    }

    @Override
    protected void shutdown() {
        getLogger().atInfo().log("================================================");
        getLogger().atInfo().log("[HyuAuth] Shutting down authentication plugin...");
        getLogger().atInfo().log("================================================");
        
        if (scheduler != null) {
            getLogger().atInfo().log("[HyuAuth] Stopping authentication loop scheduler...");
            scheduler.shutdown();
            getLogger().atInfo().log("[HyuAuth] Scheduler stopped");
        }
        
        if (authManager != null) {
            getLogger().atInfo().log("[HyuAuth] Shutting down database manager...");
            authManager.shutdown();
            getLogger().atInfo().log("[HyuAuth] Database manager shut down");
        }
        
        getLogger().atInfo().log("================================================");
        getLogger().atInfo().log("[HyuAuth] Plugin shut down successfully!");
        getLogger().atInfo().log("================================================");
    }
}
