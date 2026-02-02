package bm.b0b0b0.hyuauth.command;

import bm.b0b0b0.hyuauth.manager.AuthManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AuthCommands {
    public static class LoginCommand extends AbstractCommand {
        private final AuthManager authManager;

        public LoginCommand(AuthManager authManager) {
            super("login", "Login command");
            this.authManager = authManager;
            setAllowsExtraArguments(true);
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext ctx) {
            CommandSender sender = ctx.sender();
            String[] args = ctx.getInputString().split(" ");
            
            UUID uuid = getUuidFromSender(sender);
            if (uuid == null) {
                return CompletableFuture.completedFuture(null);
            }
            
            if (authManager.isLoggedIn(uuid)) {
                sender.sendMessage(Message.translation("hyuauth.commands.login.already_logged_in").color("red"));
                return CompletableFuture.completedFuture(null);
            }
            
            if (args.length < 2) {
                sender.sendMessage(Message.translation("hyuauth.commands.login.usage").color("#FFFF00"));
                return CompletableFuture.completedFuture(null);
            }
            
            String password = args[1];
            
            return authManager.isRegistered(uuid).thenCompose(registered -> {
                if (!registered) {
                    sender.sendMessage(Message.translation("hyuauth.commands.login.not_registered").color("#FF0000"));
                    return CompletableFuture.completedFuture(null);
                }
                
                return authManager.login(uuid, password).thenAccept(success -> {
                    if (success) {
                        String playerName = sender instanceof PlayerRef ? ((PlayerRef) sender).getUsername() : uuid.toString();
                        System.out.println("[HyuAuth] Player logged in: " + playerName + " (" + uuid + ")");
                        sender.sendMessage(Message.translation("hyuauth.commands.login.success").color("#00FF00"));
                    } else {
                        String playerName = sender instanceof PlayerRef ? ((PlayerRef) sender).getUsername() : uuid.toString();
                        System.out.println("[HyuAuth] Login failed for: " + playerName + " (" + uuid + ")");
                        sender.sendMessage(Message.translation("hyuauth.commands.login.failed").color("#FF0000"));
                    }
                });
            });
        }
    }

    public static class LoginShortCommand extends AbstractCommand {
        private final AuthManager authManager;

        public LoginShortCommand(AuthManager authManager) {
            super("l", "Login command shortcut");
            this.authManager = authManager;
            setAllowsExtraArguments(true);
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext ctx) {
            return new LoginCommand(authManager).execute(ctx);
        }
    }

    public static class RegisterCommand extends AbstractCommand {
        private final AuthManager authManager;

        public RegisterCommand(AuthManager authManager) {
            super("register", "Register command");
            this.authManager = authManager;
            setAllowsExtraArguments(true);
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext ctx) {
            CommandSender sender = ctx.sender();
            String[] args = ctx.getInputString().split(" ");
            
            UUID uuid = getUuidFromSender(sender);
            if (uuid == null) {
                return CompletableFuture.completedFuture(null);
            }
            
            if (authManager.isLoggedIn(uuid)) {
                sender.sendMessage(Message.translation("hyuauth.commands.register.already_logged_in").color("red"));
                return CompletableFuture.completedFuture(null);
            }
            
            if (args.length < 2) {
                sender.sendMessage(Message.translation("hyuauth.commands.register.usage").color("#FFFF00"));
                return CompletableFuture.completedFuture(null);
            }
            
            String password = args[1];
            
            return authManager.isRegistered(uuid).thenCompose(registered -> {
                if (registered) {
                    sender.sendMessage(Message.translation("hyuauth.commands.register.already_registered").color("#FF0000"));
                    return CompletableFuture.completedFuture(null);
                }
                
                return authManager.register(uuid, password).thenRun(() -> {
                    String playerName = sender instanceof PlayerRef ? ((PlayerRef) sender).getUsername() : uuid.toString();
                    System.out.println("[HyuAuth] Player registered: " + playerName + " (" + uuid + ")");
                    sender.sendMessage(Message.translation("hyuauth.commands.register.success").color("#00FF00"));
                });
            });
        }
    }

    public static class RegisterShortCommand extends AbstractCommand {
        private final AuthManager authManager;

        public RegisterShortCommand(AuthManager authManager) {
            super("reg", "Register command shortcut");
            this.authManager = authManager;
            setAllowsExtraArguments(true);
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext ctx) {
            return new RegisterCommand(authManager).execute(ctx);
        }
    }

    public static class LogoutCommand extends AbstractCommand {
        private final AuthManager authManager;

        public LogoutCommand(AuthManager authManager) {
            super("logout", "Logout command");
            this.authManager = authManager;
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext ctx) {
            CommandSender sender = ctx.sender();
            
            UUID uuid = getUuidFromSender(sender);
            if (uuid == null) {
                return CompletableFuture.completedFuture(null);
            }
            
            if (!authManager.isLoggedIn(uuid)) {
                sender.sendMessage(Message.translation("hyuauth.commands.logout.not_logged_in").color("red"));
                return CompletableFuture.completedFuture(null);
            }
            
            authManager.logout(uuid);
            String playerName = sender instanceof PlayerRef ? ((PlayerRef) sender).getUsername() : uuid.toString();
            System.out.println("[HyuAuth] Player logged out: " + playerName + " (" + uuid + ")");
            sender.sendMessage(Message.translation("hyuauth.commands.logout.success").color("green"));
            
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class AuthResetCommand extends AbstractCommand {
        private final AuthManager authManager;

        public AuthResetCommand(AuthManager authManager) {
            super("authreset", "Reset player account");
            this.authManager = authManager;
            setAllowsExtraArguments(true);
            requirePermission("hytale.admin");
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext ctx) {
            CommandSender sender = ctx.sender();
            String[] args = ctx.getInputString().split(" ");
            
            if (args.length < 2) {
                sender.sendMessage(Message.translation("hyuauth.commands.authreset.usage").color("yellow"));
                return CompletableFuture.completedFuture(null);
            }
            
            String playerName = args[1];
            UUID targetUuid = findPlayerUuid(playerName);
            
            if (targetUuid == null) {
                String message = Message.translation("hyuauth.commands.authreset.player_not_found").getAnsiMessage();
                message = message.replace("{0}", playerName);
                sender.sendMessage(Message.raw(message).color("red"));
                return CompletableFuture.completedFuture(null);
            }
            
            return authManager.resetAccount(targetUuid).thenRun(() -> {
                String adminName = sender instanceof PlayerRef ? ((PlayerRef) sender).getUsername() : "Console";
                System.out.println("[HyuAuth] Account reset by " + adminName + ": " + playerName + " (" + targetUuid + ")");
                String successMsg = Message.translation("hyuauth.commands.authreset.success").getAnsiMessage();
                successMsg = successMsg.replace("{0}", playerName);
                sender.sendMessage(Message.raw(successMsg).color("green"));
            });
        }

        private UUID findPlayerUuid(String playerName) {
            List<com.hypixel.hytale.server.core.universe.PlayerRef> players = com.hypixel.hytale.server.core.universe.Universe.get().getPlayers();
            for (com.hypixel.hytale.server.core.universe.PlayerRef playerRef : players) {
                if (playerRef.getUsername().equalsIgnoreCase(playerName)) {
                    return getUuidFromPlayerRef(playerRef);
                }
            }
            return null;
        }
    }

    public static class AuthConfigCommand extends AbstractCommand {
        private final AuthManager authManager;

        public AuthConfigCommand(AuthManager authManager) {
            super("authconfig", "Configure login timeout");
            this.authManager = authManager;
            setAllowsExtraArguments(true);
            requirePermission("hytale.admin");
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext ctx) {
            CommandSender sender = ctx.sender();
            String[] args = ctx.getInputString().split(" ");
            
            if (args.length < 2) {
                sender.sendMessage(Message.translation("hyuauth.commands.authconfig.usage").color("yellow"));
                String currentMsg = Message.translation("hyuauth.commands.authconfig.current").getAnsiMessage();
                currentMsg = currentMsg.replace("{0}", String.valueOf(authManager.getTimeout()));
                sender.sendMessage(Message.raw(currentMsg).color("yellow"));
                return CompletableFuture.completedFuture(null);
            }
            
            try {
                int seconds = Integer.parseInt(args[1]);
                authManager.setTimeout(seconds);
                String adminName = sender instanceof PlayerRef ? ((PlayerRef) sender).getUsername() : "Console";
                System.out.println("[HyuAuth] Timeout configured by " + adminName + ": " + seconds + " seconds");
                String successMsg = Message.translation("hyuauth.commands.authconfig.success").getAnsiMessage();
                successMsg = successMsg.replace("{0}", String.valueOf(seconds));
                sender.sendMessage(Message.raw(successMsg).color("green"));
            } catch (NumberFormatException e) {
                sender.sendMessage(Message.translation("hyuauth.commands.authconfig.invalid_number").color("red"));
            }
            
            return CompletableFuture.completedFuture(null);
        }
    }

    private static UUID getUuidFromSender(CommandSender sender) {
        if (sender instanceof PlayerRef) {
            return getUuidFromPlayerRef((PlayerRef) sender);
        }
        if (sender instanceof com.hypixel.hytale.server.core.entity.entities.Player) {
            return getUuidFromPlayer((com.hypixel.hytale.server.core.entity.entities.Player) sender);
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private static UUID getUuidFromPlayerRef(PlayerRef playerRef) {
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
    private static UUID getUuidFromPlayer(com.hypixel.hytale.server.core.entity.entities.Player player) {
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
