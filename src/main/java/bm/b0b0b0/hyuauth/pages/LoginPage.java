package bm.b0b0b0.hyuauth.pages;

import bm.b0b0b0.hyuauth.data.AuthResult;
import bm.b0b0b0.hyuauth.services.AuthService;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import javax.annotation.Nonnull;

public class LoginPage extends InteractiveCustomUIPage<LoginPage.LoginEventData> {
    private String inputPassword = null;
    private Timer timeoutTimer = null;

    public LoginPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CantClose, LoginEventData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("LoginPage.ui");
        this.startTimeoutTimer(ref, store);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PasswordField", EventData.of("@Password", "#PasswordField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#LoginGroup #SubmitButton", EventData.of("Button", "SubmitButton"), true);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#LoginGroup #CancelButton", EventData.of("Button", "CancelButton"), true);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull LoginEventData data) {
        super.handleDataEvent(ref, store, data);
        if (data.password != null) {
            this.inputPassword = data.password;
        } else {
            Player playerComponent = store.getComponent(ref, Player.getComponentType());
            if (playerComponent != null && data.pressedButton != null) {
                if (data.pressedButton.equals("CancelButton")) {
                    this.playerRef.getPacketHandler().disconnect("You cannot play in this server without login");
                } else if (data.pressedButton.equals("SubmitButton")) {
                    this.handlePasswordAuthentication(ref, store, playerComponent);
                } else {
                    this.displayError(getLocalizedText("hyuauth.messages.unknown_error"));
                }
            }
        }
    }

    private void handlePasswordAuthentication(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull Player playerComponent) {
        if (this.inputPassword != null && !this.inputPassword.isEmpty()) {
            AuthResult result = AuthService.GetInstance().AuthenticatePlayer(this.playerRef.getUuid(), this.inputPassword);
            this.handleAuthenticationResult(ref, store, this.playerRef, playerComponent, result);
        } else {
            this.displayError(getLocalizedText("hyuauth.messages.invalid_password"));
        }
    }

    private void handleAuthenticationResult(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PlayerRef playerRef, @Nonnull Player playerComponent, @Nonnull AuthResult result) {
        if (!result.Success.booleanValue()) {
            String errorMessage = getLocalizedErrorMessage(result.Message);
            this.displayError(errorMessage);
        } else {
            com.hypixel.hytale.server.core.Message successMessage = com.hypixel.hytale.server.core.Message.translation("hyuauth.messages.login_success")
                    .color("#00FF00")
                    .bold(true);
            playerComponent.sendMessage(successMessage);
            this.cancelTimeout();
            this.close();
        }
    }

    private void startTimeoutTimer(final @Nonnull Ref<EntityStore> ref, final @Nonnull Store<EntityStore> store) {
        this.cancelTimeout();
        int timeoutSeconds = 60;
        final World world = store.getExternalData().getWorld();
        System.out.println("[HyuAuth] [LoginPage] Starting timeout timer for " + timeoutSeconds + " seconds");
        this.timeoutTimer = new Timer(true);
        this.timeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                world.execute(() -> {
                    Player playerComponent = store.getComponent(ref, Player.getComponentType());
                    if (playerComponent != null) {
                        @SuppressWarnings("deprecation")
                        UUID playerUuid = LoginPage.this.playerRef.getUuid();
                        Boolean isAuthenticated = AuthService.GetInstance().GetPlayer(playerUuid);
                        System.out.println("[HyuAuth] [LoginPage] Timeout check - UUID: " + playerUuid + ", isAuthenticated: " + isAuthenticated);
                        if (isAuthenticated == null || !isAuthenticated) {
                            System.out.println("[HyuAuth] [LoginPage] Kicking player due to timeout");
                            LoginPage.this.playerRef.getPacketHandler().disconnect(Message.translation("hyuauth.messages.timeout_kick").getAnsiMessage());
                        } else {
                            System.out.println("[HyuAuth] [LoginPage] Player is authenticated, not kicking");
                        }
                    }
                });
            }
        }, (long) timeoutSeconds * 1000L);
    }

    private void cancelTimeout() {
        if (this.timeoutTimer != null) {
            this.timeoutTimer.cancel();
            this.timeoutTimer = null;
        }
    }

    private void displayError(@Nonnull String errorMessage) {
        UICommandBuilder commandBuilder = new UICommandBuilder();
        String errorSelector = "#LoginGroup #Error";
        commandBuilder.set(errorSelector + ".Visible", true);
        commandBuilder.set(errorSelector + ".Text", errorMessage);
        this.sendUpdate(commandBuilder);
    }

    private String getLocalizedText(String key) {
        return getFallbackText(key);
    }

    private String getFallbackText(String key) {
        if (key.equals("hyuauth.messages.invalid_password")) return "Неверный пароль";
        if (key.equals("hyuauth.messages.authentication_failed")) return "Ошибка авторизации";
        if (key.equals("hyuauth.messages.error_1002")) return "Ошибка 1002";
        if (key.equals("hyuauth.messages.unknown_error")) return "Неизвестная ошибка";
        return key;
    }

    private String getLocalizedErrorMessage(String englishMessage) {
        if (englishMessage == null) {
            return getLocalizedText("hyuauth.messages.unknown_error");
        }
        if (englishMessage.equals("Invalid password")) {
            return getLocalizedText("hyuauth.messages.invalid_password");
        }
        if (englishMessage.equals("Error 1002")) {
            return getLocalizedText("hyuauth.messages.error_1002");
        }
        if (englishMessage.startsWith("Authentication failed")) {
            return getLocalizedText("hyuauth.messages.authentication_failed");
        }
        return getLocalizedText("hyuauth.messages.unknown_error");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static class LoginEventData {
        private static final String KEY_PASSWORD = "@Password";
        private static final String KEY_BUTTON = "Button";
        public static final BuilderCodec<LoginEventData> CODEC = ((BuilderCodec.Builder)((BuilderCodec.Builder)BuilderCodec.builder(LoginEventData.class, LoginEventData::new).append(new KeyedCodec<String>("@Password", Codec.STRING), (data, value) -> {
            ((LoginEventData)data).password = (String)value;
        }, data -> ((LoginEventData)data).password).add()).append(new KeyedCodec<String>("Button", Codec.STRING), (data, value) -> {
            ((LoginEventData)data).pressedButton = (String)value;
        }, data -> ((LoginEventData)data).pressedButton).add()).build();
        public String password;
        public String pressedButton;
    }
}
