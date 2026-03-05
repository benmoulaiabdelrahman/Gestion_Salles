package com.gestion.salles.utils;

import java.util.logging.Logger;

public final class SessionContext {
    private static final Logger LOGGER = Logger.getLogger(SessionContext.class.getName());
    private static final ThreadLocal<Integer> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USER_EMAIL = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_SESSION_TOKEN = new ThreadLocal<>();
    private static final ThreadLocal<Integer> PASSWORD_ATTEMPTS = ThreadLocal.withInitial(() -> 0);

    private SessionContext() {
    }

    public static void setCurrentUser(int id, String email) {
        setCurrentUser(id, email, null);
    }

    public static void setCurrentUser(int id, String email, String sessionToken) {
        CURRENT_USER_ID.set(id);
        CURRENT_USER_EMAIL.set(email);
        CURRENT_SESSION_TOKEN.set(sessionToken);
    }

    public static Integer getCurrentUserId() {
        return CURRENT_USER_ID.get();
    }

    public static String getCurrentUserEmail() {
        return CURRENT_USER_EMAIL.get();
    }

    public static String getCurrentSessionToken() {
        return CURRENT_SESSION_TOKEN.get();
    }

    public static boolean isAuthenticated() {
        return CURRENT_USER_ID.get() != null;
    }

    public static void requireAuthenticated() {
        if (!isAuthenticated()) {
            throw new SecurityException("Operation requires an authenticated user.");
        }

        String email = CURRENT_USER_EMAIL.get();
        String token = CURRENT_SESSION_TOKEN.get();
        if (email != null && token != null && !token.isBlank()) {
            if (!SessionManager.getInstance().isSessionValid(email, token)) {
                clear();
                throw new SecurityException("Session is no longer valid.");
            }
            return;
        }

        if (email != null) {
            LOGGER.fine("Authenticated context for " + email + " has no session token in thread-local state; skipping DB cross-check.");
        }
    }

    public static void set(int id) {
        setCurrentUser(id, null);
    }

    public static int get() {
        Integer id = getCurrentUserId();
        if (id == null) {
            throw new IllegalStateException("SessionContext not set for current thread.");
        }
        return id;
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
        CURRENT_USER_EMAIL.remove();
        CURRENT_SESSION_TOKEN.remove();
        PASSWORD_ATTEMPTS.remove();
    }

    public static int incrementPasswordAttempts() {
        int updated = getPasswordAttempts() + 1;
        PASSWORD_ATTEMPTS.set(updated);
        return updated;
    }

    public static int getPasswordAttempts() {
        Integer attempts = PASSWORD_ATTEMPTS.get();
        return attempts != null ? attempts : 0;
    }

    public static void resetPasswordAttempts() {
        PASSWORD_ATTEMPTS.set(0);
    }
}
