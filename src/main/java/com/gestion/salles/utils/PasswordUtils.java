package com.gestion.salles.utils;

/******************************************************************************
 * PasswordUtils.java
 *
 * Utility class for hashing and verifying passwords using BCrypt, and for
 * generating cryptographically secure random passwords.
 ******************************************************************************/

import org.mindrot.jbcrypt.BCrypt;

import java.security.SecureRandom;

public class PasswordUtils {

    private static final int    BCRYPT_WORK_FACTOR = 10;
    private static final String CHAR_LOWER         = "abcdefghijklmnopqrstuvwxyz";
    private static final String CHAR_UPPER         = CHAR_LOWER.toUpperCase();
    private static final String NUMBER             = "0123456789";
    private static final String OTHER_CHAR         = "!@#$%&*()_+-=[]?";
    private static final String PASSWORD_CHARS     = CHAR_LOWER + CHAR_UPPER + NUMBER + OTHER_CHAR;
    private static final SecureRandom random       = new SecureRandom();

    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_WORK_FACTOR));
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) return false;
        try {
            return isBCryptHash(hashedPassword) && BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid password hash format: " + e.getMessage());
            return false;
        }
    }

    public static boolean isBCryptHash(String hash) {
        return hash != null && (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
    }

    public static String generateRandomPassword(int length) {
        if (length < 8) {
            throw new IllegalArgumentException("Password length must be at least 8 characters.");
        }

        StringBuilder password = new StringBuilder(length);
        password.append(CHAR_LOWER.charAt(random.nextInt(CHAR_LOWER.length())));
        password.append(CHAR_UPPER.charAt(random.nextInt(CHAR_UPPER.length())));
        password.append(NUMBER.charAt(random.nextInt(NUMBER.length())));
        password.append(OTHER_CHAR.charAt(random.nextInt(OTHER_CHAR.length())));

        for (int i = 4; i < length; i++) {
            password.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        }

        char[] chars = password.toString().toCharArray();
        for (int i = 0; i < length; i++) {
            int j = random.nextInt(length);
            char tmp = chars[i]; chars[i] = chars[j]; chars[j] = tmp;
        }

        return new String(chars);
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            String password = args[0];
            String hash     = hashPassword(password);
            System.out.println("Plain text : " + password);
            System.out.println("BCrypt hash: " + hash);
            System.out.println("Verified   : " + verifyPassword(password, hash));
        } else {
            System.err.println("Usage: java PasswordUtils <password>");
            System.exit(1);
        }
    }
}
