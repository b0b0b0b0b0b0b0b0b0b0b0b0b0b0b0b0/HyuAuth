package bm.b0b0b0.hyuauth;

import bm.b0b0b0.hyuauth.command.AuthCommands;
import bm.b0b0b0.hyuauth.config.ConfigManager;
import bm.b0b0b0.hyuauth.events.PlayerAuthEvents;
import bm.b0b0b0.hyuauth.events.PlayerChatListener;
import bm.b0b0b0.hyuauth.manager.AuthManager;
import bm.b0b0b0.hyuauth.services.AuthService;
import bm.b0b0b0.hyuauth.session.SessionManager;
import bm.b0b0b0.hyuauth.system.PlayerAuthMovementSystem;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerSetupConnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;

public class HyuAuthPlugin extends JavaPlugin {
    private AuthManager authManager;
    private ConfigManager configManager;
    private PlayerAuthEvents playerAuthEvents;
    private PlayerChatListener playerChatListener;
    private SessionManager sessionManager;
    private PlayerAuthMovementSystem movementSystem;

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
        
        getLogger().atInfo().log("[HyuAuth] Initializing AuthService...");
        new AuthService().Initialize(getDataDirectory(), configManager.getDatabaseFileName());
        getLogger().atInfo().log("[HyuAuth] AuthService initialized");
        
        getLogger().atInfo().log("[HyuAuth] Initializing database manager...");
        authManager = new AuthManager(getDataDirectory(), configManager);
        getLogger().atInfo().log("[HyuAuth] Database manager initialized");
        
        getLogger().atInfo().log("[HyuAuth] Initializing session manager...");
        sessionManager = new SessionManager(configManager.getSessionTimeoutMinutes());
        getLogger().atInfo().log("[HyuAuth] Session manager initialized");
        
        getLogger().atInfo().log("[HyuAuth] Initializing movement system...");
        movementSystem = new PlayerAuthMovementSystem(sessionManager);
        getLogger().atInfo().log("[HyuAuth] Movement system initialized");
        
        getLogger().atInfo().log("[HyuAuth] Initializing player auth events...");
        playerAuthEvents = new PlayerAuthEvents(sessionManager);
        PlayerAuthEvents.setMovementSystem(movementSystem);
        getLogger().atInfo().log("[HyuAuth] Player auth events initialized");
        
        getLogger().atInfo().log("[HyuAuth] Initializing chat listener...");
        playerChatListener = new PlayerChatListener();
        getLogger().atInfo().log("[HyuAuth] Chat listener initialized");
        
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
        getEventRegistry().registerGlobal(PlayerSetupConnectEvent.class, playerAuthEvents::onPlayerSetupConnect);
        getEventRegistry().registerGlobal(PlayerConnectEvent.class, playerAuthEvents::onPlayerConnect);
        getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, playerAuthEvents::onAddPlayerToWorld);
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, playerAuthEvents::onPlayerReady);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, playerAuthEvents::onPlayerDisconnect);
        getEventRegistry().registerGlobal(DrainPlayerFromWorldEvent.class, playerAuthEvents::onPlayerQuit);
        
        try {
            Class<?> chatEventClass = Class.forName("com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent");
            getEventRegistry().registerGlobal(chatEventClass, playerChatListener::onPlayerChat);
            getLogger().atInfo().log("[HyuAuth] Chat event listener registered: PlayerChatEvent");
        } catch (ClassNotFoundException e) {
            System.out.println("[HyuAuth] PlayerChatEvent not found, trying alternatives...");
            try {
                Class<?> chatEventClass = Class.forName("com.hypixel.hytale.server.core.event.events.ChatEvent");
                getEventRegistry().registerGlobal(chatEventClass, playerChatListener::onPlayerChat);
                getLogger().atInfo().log("[HyuAuth] Chat event listener registered: ChatEvent");
            } catch (ClassNotFoundException e2) {
                System.out.println("[HyuAuth] ChatEvent not found, trying MessageEvent...");
                try {
                    Class<?> chatEventClass = Class.forName("com.hypixel.hytale.server.core.event.events.player.MessageEvent");
                    getEventRegistry().registerGlobal(chatEventClass, playerChatListener::onPlayerChat);
                    getLogger().atInfo().log("[HyuAuth] Chat event listener registered: MessageEvent");
                } catch (ClassNotFoundException e3) {
                    System.out.println("[HyuAuth] MessageEvent not found, trying PlayerMessageEvent...");
                    try {
                        Class<?> chatEventClass = Class.forName("com.hypixel.hytale.server.core.event.events.player.PlayerMessageEvent");
                        getEventRegistry().registerGlobal(chatEventClass, playerChatListener::onPlayerChat);
                        getLogger().atInfo().log("[HyuAuth] Chat event listener registered: PlayerMessageEvent");
                    } catch (ClassNotFoundException e4) {
                        getLogger().atInfo().log("[HyuAuth] Could not find chat event class, chat blocking disabled");
                        System.out.println("[HyuAuth] ERROR: Could not find any chat event class!");
                    }
                }
            }
        }
        
        getLogger().atInfo().log("[HyuAuth] Event listeners registered: PlayerSetupConnectEvent, PlayerConnectEvent, AddPlayerToWorldEvent, PlayerReadyEvent, PlayerDisconnectEvent, DrainPlayerFromWorldEvent, PlayerChatEvent");
        
        getLogger().atInfo().log("[HyuAuth] Setup completed successfully!");
        getLogger().atInfo().log("================================================");
    }

    @Override
    protected void start() {
        getLogger().atInfo().log("================================================");
        getLogger().atInfo().log("[HyuAuth] Starting authentication plugin...");
        getLogger().atInfo().log("================================================");
        
        getLogger().atInfo().log("================================================");
        getLogger().atInfo().log("[HyuAuth] Plugin started successfully!");
        getLogger().atInfo().log("[HyuAuth] Players will see authentication UI window");
        getLogger().atInfo().log("================================================");
    }


    public SessionManager getSessionManager() {
        return sessionManager;
    }

    @Override
    protected void shutdown() {
        getLogger().atInfo().log("================================================");
        getLogger().atInfo().log("[HyuAuth] Shutting down authentication plugin...");
        getLogger().atInfo().log("================================================");
        
        if (AuthService.GetInstance() != null) {
            getLogger().atInfo().log("[HyuAuth] Shutting down AuthService...");
            AuthService.GetInstance().shutdown();
            getLogger().atInfo().log("[HyuAuth] AuthService shut down");
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
