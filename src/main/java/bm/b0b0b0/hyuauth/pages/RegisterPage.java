package bm.b0b0b0.hyuauth.pages;

import bm.b0b0b0.hyuauth.data.AuthResult;
import bm.b0b0b0.hyuauth.services.AuthService;
import bm.b0b0b0.hyuauth.session.SessionManager;
import bm.b0b0b0.hyuauth.system.PlayerAuthMovementSystem;
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

public class RegisterPage extends InteractiveCustomUIPage<RegisterPage.RegisterEventData> {
    private String inputPassword = null;
    private String inputConfirmPassword = null;
    private Timer timeoutTimer = null;
    private final SessionManager sessionManager;

    public RegisterPage(@Nonnull PlayerRef playerRef, SessionManager sessionManager) {
        super(playerRef, CustomPageLifetime.CantClose, RegisterEventData.CODEC);
        this.sessionManager = sessionManager;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("RegisterPage.ui");
        this.startTimeoutTimer(ref, store);
        @SuppressWarnings("deprecation")
        UUID playerUuid = this.playerRef.getUuid();
        PlayerAuthMovementSystem.markPageOpen(playerUuid);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PasswordField", EventData.of("@Password", "#PasswordField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ConfirmPasswordField", EventData.of("@ConfirmPassword", "#ConfirmPasswordField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SubmitButton", EventData.of("Button", "SubmitButton"), true);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelButton", EventData.of("Button", "CancelButton"), true);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull RegisterEventData data) {
        super.handleDataEvent(ref, store, data);
        if (data.Password != null) {
            this.inputPassword = data.Password;
        } else if (data.ConfirmPassword != null) {
            this.inputConfirmPassword = data.ConfirmPassword;
        } else {
            Player playerComponent = store.getComponent(ref, Player.getComponentType());
            if (playerComponent != null && data.PressedButton != null) {
                if (data.PressedButton.equals("CancelButton")) {
                    @SuppressWarnings("deprecation")
                    UUID playerUuid = this.playerRef.getUuid();
                    PlayerAuthMovementSystem.markPageClosed(playerUuid);
                    this.cancelTimeout();
                    this.playerRef.getPacketHandler().disconnect("Вам нужно зарегистрироваться для игры на этом сервере.");
                } else if (data.PressedButton.equals("SubmitButton")) {
                    System.out.println("[HyuAuth] [RegisterPage] Submit button pressed, validating fields...");
                    if (this.validateFields()) {
                        System.out.println("[HyuAuth] [RegisterPage] Validation passed, calling Register");
                        String username = this.playerRef.getUsername();
                        @SuppressWarnings("deprecation")
                        java.util.UUID playerUuid = this.playerRef.getUuid();
                        System.out.println("[HyuAuth] [RegisterPage] Calling AuthService.Register for UUID: " + playerUuid + ", username: " + username);
                        AuthResult result = AuthService.GetInstance().Register(playerUuid, username, this.inputPassword);
                        System.out.println("[HyuAuth] [RegisterPage] Register returned - Success: " + result.Success + ", Message: " + result.Message);
                        this.handleRegistrationResult(ref, store, this.playerRef, playerComponent, result);
                    } else {
                        System.out.println("[HyuAuth] [RegisterPage] Validation failed");
                    }
                } else {
                    this.displayError(getLocalizedText("hyuauth.messages.unknown_error"));
                }
            }
        }
    }

    private boolean validateFields() {
        if (this.inputPassword == null || this.inputPassword.isEmpty()) {
            this.displayError(getLocalizedText("hyuauth.messages.password_empty"));
            return false;
        }
        if (this.inputPassword.length() < 3) {
            this.displayError(getLocalizedText("hyuauth.messages.password_too_short"));
            return false;
        }
        if (this.inputPassword.length() > 64) {
            this.displayError(getLocalizedText("hyuauth.messages.password_too_long"));
            return false;
        }
        if (this.inputConfirmPassword == null || this.inputConfirmPassword.isEmpty()) {
            this.displayError(getLocalizedText("hyuauth.messages.password_confirm_empty"));
            return false;
        }
        if (!this.inputPassword.equals(this.inputConfirmPassword)) {
            this.displayError(getLocalizedText("hyuauth.messages.password_mismatch"));
            return false;
        }
        return true;
    }

    protected void handleRegistrationResult(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PlayerRef playerRef, @Nonnull Player playerComponent, @Nonnull AuthResult result) {
        System.out.println("[HyuAuth] [RegisterPage] handleRegistrationResult called - Success: " + result.Success + ", Message: " + result.Message);
        if (!result.Success.booleanValue()) {
            System.out.println("[HyuAuth] [RegisterPage] Registration failed, displaying error");
            String errorMessage = getLocalizedErrorMessage(result.Message);
            this.displayError(errorMessage);
        } else {
            System.out.println("[HyuAuth] [RegisterPage] Registration successful, closing page");
            @SuppressWarnings("deprecation")
            UUID playerUuid = playerRef.getUuid();
            String username = playerRef.getUsername();
            String ipAddress = getPlayerIP(playerRef);
            
            if (this.sessionManager != null) {
                this.sessionManager.createSession(ipAddress, username, playerUuid);
            }
            
            com.hypixel.hytale.server.core.Message successMessage = com.hypixel.hytale.server.core.Message.raw("Регистрация успешна!")
                    .color("#00FF00")
                    .bold(true);
            System.out.println("[HyuAuth] [RegisterPage] Sending success message to player");
            playerComponent.sendMessage(successMessage);
            System.out.println("[HyuAuth] [RegisterPage] Success message sent, closing page");
            PlayerAuthMovementSystem.markPageClosed(playerUuid);
            this.cancelTimeout();
            this.close();
            System.out.println("[HyuAuth] [RegisterPage] Page closed");
        }
    }

    private String getPlayerIP(@Nonnull PlayerRef playerRef) {
        try {
            com.hypixel.hytale.server.core.io.PacketHandler packetHandler = playerRef.getPacketHandler();
            if (packetHandler != null) {
                try {
                    java.lang.reflect.Method getConnectionMethod = packetHandler.getClass().getMethod("getConnection");
                    getConnectionMethod.setAccessible(true);
                    Object connection = getConnectionMethod.invoke(packetHandler);
                    if (connection != null) {
                        try {
                            java.lang.reflect.Method getRemoteAddressMethod = connection.getClass().getMethod("getRemoteAddress");
                            getRemoteAddressMethod.setAccessible(true);
                            Object address = getRemoteAddressMethod.invoke(connection);
                            if (address != null) {
                                String addressStr = address.toString();
                                if (addressStr.contains("/")) {
                                    return addressStr.split("/")[1].split(":")[0];
                                }
                                return addressStr.split(":")[0];
                            }
                        } catch (Exception ignored) {
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            System.out.println("[HyuAuth] [RegisterPage] Error getting IP: " + e.getMessage());
        }
        return "unknown";
    }


    private void startTimeoutTimer(final @Nonnull Ref<EntityStore> ref, final @Nonnull Store<EntityStore> store) {
        this.cancelTimeout();
        int timeoutSeconds = 60;
        final World world = store.getExternalData().getWorld();
        System.out.println("[HyuAuth] [RegisterPage] Starting timeout timer for " + timeoutSeconds + " seconds");
        this.timeoutTimer = new Timer(true);
        this.timeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                world.execute(() -> {
                    try {
                        if (!ref.isValid()) {
                            System.out.println("[HyuAuth] [RegisterPage] Ref is invalid, cancelling timeout check");
                            return;
                        }
                        Player playerComponent = store.getComponent(ref, Player.getComponentType());
                        if (playerComponent != null) {
                            @SuppressWarnings("deprecation")
                            UUID playerUuid = RegisterPage.this.playerRef.getUuid();
                            Boolean isAuthenticated = AuthService.GetInstance().GetPlayer(playerUuid);
                            System.out.println("[HyuAuth] [RegisterPage] Timeout check - UUID: " + playerUuid + ", isAuthenticated: " + isAuthenticated);
                            if (isAuthenticated == null || !isAuthenticated) {
                                System.out.println("[HyuAuth] [RegisterPage] Kicking player due to timeout");
                                RegisterPage.this.playerRef.getPacketHandler().disconnect("Время на вход истекло! Пожалуйста, переподключитесь.");
                            } else {
                                System.out.println("[HyuAuth] [RegisterPage] Player is authenticated, not kicking");
                            }
                        }
                    } catch (IllegalStateException e) {
                        System.out.println("[HyuAuth] [RegisterPage] Ref is invalid during timeout check: " + e.getMessage());
                    } catch (Exception e) {
                        System.out.println("[HyuAuth] [RegisterPage] Error in timeout check: " + e.getMessage());
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
        commandBuilder.set("#Error.Visible", true);
        commandBuilder.set("#Error.Text", errorMessage);
        this.sendUpdate(commandBuilder);
    }

    private String getLocalizedText(String key) {
        return getFallbackText(key);
    }

    private String getFallbackText(String key) {
        if (key.equals("hyuauth.messages.invalid_password")) return "Неверный пароль";
        if (key.equals("hyuauth.messages.password_empty")) return "Пожалуйста, введите пароль";
        if (key.equals("hyuauth.messages.password_too_short")) return "Пароль должен содержать минимум 3 символа";
        if (key.equals("hyuauth.messages.password_too_long")) return "Пароль не должен превышать 64 символа";
        if (key.equals("hyuauth.messages.password_confirm_empty")) return "Необходимо подтвердить пароль";
        if (key.equals("hyuauth.messages.password_mismatch")) return "Пароль и подтверждение должны совпадать";
        if (key.equals("hyuauth.messages.registration_failed")) return "Ошибка регистрации";
        if (key.equals("hyuauth.messages.authentication_failed")) return "Ошибка авторизации";
        if (key.equals("hyuauth.messages.error_1002")) return "Ошибка 1002";
        if (key.equals("hyuauth.messages.username_already_registered")) return "Имя пользователя уже зарегистрировано";
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
        if (englishMessage.equals("Username already registered.")) {
            return getLocalizedText("hyuauth.messages.username_already_registered");
        }
        if (englishMessage.startsWith("Registration failed")) {
            return getLocalizedText("hyuauth.messages.registration_failed");
        }
        if (englishMessage.equals("Error 1002")) {
            return getLocalizedText("hyuauth.messages.error_1002");
        }
        return getLocalizedText("hyuauth.messages.unknown_error");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static class RegisterEventData {
        private static final String KEY_PASSWORD = "@Password";
        private static final String KEY_CONFIRM_PASSWORD = "@ConfirmPassword";
        private static final String KEY_BUTTON = "Button";
        public static final BuilderCodec<RegisterEventData> CODEC = ((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)BuilderCodec.builder(RegisterEventData.class, RegisterEventData::new).append(new KeyedCodec<String>("@Password", Codec.STRING), (data, value) -> {
            ((RegisterEventData)data).Password = (String)value;
        }, data -> ((RegisterEventData)data).Password).add()).append(new KeyedCodec<String>("@ConfirmPassword", Codec.STRING), (data, value) -> {
            ((RegisterEventData)data).ConfirmPassword = (String)value;
        }, data -> ((RegisterEventData)data).ConfirmPassword).add()).append(new KeyedCodec<String>("Button", Codec.STRING), (data, value) -> {
            ((RegisterEventData)data).PressedButton = (String)value;
        }, data -> ((RegisterEventData)data).PressedButton).add()).build();
        public String Password;
        public String ConfirmPassword;
        public String PressedButton;
    }
}
