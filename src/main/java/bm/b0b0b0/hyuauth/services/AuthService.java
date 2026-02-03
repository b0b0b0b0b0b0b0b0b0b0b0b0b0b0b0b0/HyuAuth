package bm.b0b0b0.hyuauth.services;

import bm.b0b0b0.hyuauth.HyuAuthPlugin;
import bm.b0b0b0.hyuauth.data.AuthResult;
import bm.b0b0b0.hyuauth.database.DatabaseManager;
import bm.b0b0b0.hyuauth.security.PasswordHasher;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;

public class AuthService {
    private static AuthService _instance;
    private final HashMap<UUID, Boolean> _authInstances = new HashMap<>();
    private DatabaseManager databaseManager;

    public AuthService Initialize(Path dataDirectory, String dbFileName) {
        if (_instance == null) {
            _instance = new AuthService();
            _instance.databaseManager = new DatabaseManager(dataDirectory, dbFileName);
            System.out.println("[HyuAuth] AuthService initialized");
        }
        return _instance;
    }

    public static AuthService GetInstance() {
        return _instance;
    }

    public Boolean GetPlayer(UUID id) {
        return _authInstances.getOrDefault(id, false);
    }

    public AuthResult Register(UUID id, String username, String password) {
        System.out.println("[HyuAuth] [AuthService] Register called for UUID: " + id + ", username: " + username);
        AuthResult result = new AuthResult();
        
        if (_authInstances.containsKey(id) && _authInstances.get(id)) {
            System.out.println("[HyuAuth] [AuthService] Register failed: Already registered");
            result.Success = false;
            result.Message = "Username already registered.";
            return result;
        }
        
        if (databaseManager.isUserRegistered(id)) {
            System.out.println("[HyuAuth] [AuthService] Register failed: Already in database");
            result.Success = false;
            result.Message = "Username already registered.";
            return result;
        }
        
        try {
            String passwordHash = PasswordHasher.hash(password);
            databaseManager.registerUser(id, username, passwordHash);
            _authInstances.put(id, true);
            result.Success = true;
            result.Message = "Registration successful.";
            System.out.println("[HyuAuth] [AuthService] Register success for UUID: " + id);
        } catch (Exception e) {
            System.out.println("[HyuAuth] [AuthService] Register error: " + e.getMessage());
            e.printStackTrace();
            result.Success = false;
            result.Message = "Registration failed: " + e.getMessage();
        }
        
        return result;
    }

    public AuthResult AuthenticatePlayer(UUID id, String password) {
        System.out.println("[HyuAuth] [AuthService] AuthenticatePlayer called for UUID: " + id);
        AuthResult result = new AuthResult();
        
        if (!databaseManager.isUserRegistered(id)) {
            System.out.println("[HyuAuth] [AuthService] AuthenticatePlayer failed: Not registered");
            result.Success = false;
            result.Message = "Error 1002";
            return result;
        }
        
        try {
            String storedHash = databaseManager.getPasswordHash(id);
            if (storedHash == null) {
                System.out.println("[HyuAuth] [AuthService] AuthenticatePlayer failed: No password hash");
                result.Success = false;
                result.Message = "Invalid password";
                return result;
            }
            
            if (PasswordHasher.verify(password, storedHash)) {
                _authInstances.put(id, true);
                result.Success = true;
                result.Message = "Login successful.";
                System.out.println("[HyuAuth] [AuthService] AuthenticatePlayer success for UUID: " + id);
            } else {
                System.out.println("[HyuAuth] [AuthService] AuthenticatePlayer failed: Invalid password");
                result.Success = false;
                result.Message = "Invalid password";
            }
        } catch (Exception e) {
            System.out.println("[HyuAuth] [AuthService] AuthenticatePlayer error: " + e.getMessage());
            e.printStackTrace();
            result.Success = false;
            result.Message = "Authentication failed: " + e.getMessage();
        }
        
        return result;
    }

    public Boolean ParseFromDatabase(UUID id) {
        boolean registered = databaseManager.isUserRegistered(id);
        if (registered) {
            _authInstances.put(id, false);
        }
        return registered;
    }

    public void ResetPlayer(UUID id) {
        _authInstances.remove(id);
        System.out.println("[HyuAuth] [AuthService] Player reset: " + id);
    }

    public void shutdown() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }
}
