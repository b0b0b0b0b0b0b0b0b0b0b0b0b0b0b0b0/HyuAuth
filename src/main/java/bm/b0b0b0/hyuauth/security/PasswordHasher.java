package bm.b0b0b0.hyuauth.security;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {
    private static final int ROUNDS = 12;

    public static String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(ROUNDS));
    }

    public static boolean verify(String password, String hash) {
        try {
            return BCrypt.checkpw(password, hash);
        } catch (Exception ignored) {
            return false;
        }
    }
}
