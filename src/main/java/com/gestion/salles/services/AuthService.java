package com.gestion.salles.services;

/******************************************************************************
 * AuthService.java
 *
 * Singleton authentication service for the Gestion des Salles application.
 * Handles credential verification, Remember Me token lifecycle, and
 * session-scoped user state. Password arrays are zeroed in finally blocks
 * at every entry point. BCrypt forces an unavoidable String conversion
 * internally; those Strings are nulled immediately after use.
 ******************************************************************************/

import com.gestion.salles.models.User;
import com.gestion.salles.utils.TokenStorage;
import com.gestion.salles.utils.UIUtils;
import com.gestion.salles.utils.PasswordUtils;
import com.gestion.salles.utils.SessionContext;
import com.gestion.salles.dao.UserDAO;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuthService {

    private static final Logger      LOGGER  = Logger.getLogger(AuthService.class.getName());
    private static final SecureRandom RANDOM  = new SecureRandom();

    private static volatile AuthService instance;

    private User currentUser;
    private final UserRepository userRepository;

    private AuthService() {
        this(new UserDAOUserRepository(new UserDAO()));
    }

    AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public static AuthService getInstance() {
        if (instance == null) {
            synchronized (AuthService.class) {
                if (instance == null) {
                    instance = new AuthService();
                }
            }
        }
        return instance;
    }

    public AuthenticationResult authenticate(String email, char[] password, boolean rememberMe) {
        if (email == null || email.trim().isEmpty() || !UIUtils.isValidEmail(email)) {
            if (password != null) Arrays.fill(password, '\0');
            return new AuthenticationResult(false, "Format d'email invalide.", null);
        }
        if (password == null || password.length == 0) {
            return new AuthenticationResult(false, "Le mot de passe est requis.", null);
        }

        try {
            User user = userRepository.authenticate(email, password);
            if (user != null) {
                currentUser = user;
                SessionContext.setCurrentUser(user.getIdUtilisateur(), user.getEmail());
                if (rememberMe) {
                    handleRememberMe(user);
                } else {
                    clearRememberMe(user.getIdUtilisateur());
                }
                LOGGER.info("User authenticated successfully: " + email);
                return new AuthenticationResult(true, "Connexion réussie.", user);
            } else {
                LOGGER.warning("Authentication failed for email: " + email);
                return new AuthenticationResult(false, "Email ou mot de passe incorrect.", null);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Authentication error for email: " + email, e);
            return new AuthenticationResult(false, "Erreur d'authentification.", null);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    public AuthenticationResult authenticateWithToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return new AuthenticationResult(false, "Token invalide ou vide.", null);
        }
        try {
            User user = userRepository.findUserByRememberToken(token);
            if (user != null) {
                currentUser = user;
                SessionContext.setCurrentUser(user.getIdUtilisateur(), user.getEmail());
                LOGGER.info("User authenticated successfully with token: " + user.getEmail());
                return new AuthenticationResult(true, "Connexion via token réussie.", user);
            } else {
                LOGGER.warning("Invalid or expired token provided. Deleting local token.");
                TokenStorage.deleteToken();
                return new AuthenticationResult(false, "Token invalide ou expiré.", null);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during token authentication", e);
            return new AuthenticationResult(false, "Erreur d'authentification par token.", null);
        }
    }

    public boolean verifyPassword(String email, char[] plainPassword) {
        if (email == null || email.trim().isEmpty()) {
            if (plainPassword != null) Arrays.fill(plainPassword, '\0');
            return false;
        }
        if (plainPassword == null || plainPassword.length == 0) {
            return false;
        }
        String plainPasswordString = null;
        try {
            User user = userRepository.findByEmail(email);
            if (user == null) return false;
            plainPasswordString = new String(plainPassword);
            return PasswordUtils.verifyPassword(plainPasswordString, user.getMotDePasse());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error verifying password for email: " + email, e);
            return false;
        } finally {
            Arrays.fill(plainPassword, '\0');
            plainPasswordString = null;
        }
    }

    public boolean updatePassword(String email, char[] newPassword) {
        if (email == null || email.trim().isEmpty()) {
            if (newPassword != null) Arrays.fill(newPassword, '\0');
            return false;
        }
        if (newPassword == null || newPassword.length == 0) {
            return false;
        }
        String newPasswordString = null;
        try {
            newPasswordString = new String(newPassword);
            return userRepository.updatePassword(email, newPasswordString);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating password for email: " + email, e);
            return false;
        } finally {
            Arrays.fill(newPassword, '\0');
            newPasswordString = null;
        }
    }

    public void logout() {
        if (currentUser != null) {
            LOGGER.info("User logging out: " + currentUser.getEmail());
            clearRememberMe(currentUser.getIdUtilisateur());
            currentUser = null;
        }
        SessionContext.clear();
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isCurrentUserAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public boolean isCurrentUserChefDepartement() {
        return currentUser != null && currentUser.isChefDepartement();
    }

    public boolean isCurrentUserEnseignant() {
        return currentUser != null && currentUser.isEnseignant();
    }

    private void handleRememberMe(User user) {
        String token  = generateNewToken();
        Timestamp expiry = Timestamp.from(Instant.now().plus(30, ChronoUnit.DAYS));
        if (userRepository.storeRememberToken(user.getIdUtilisateur(), token, expiry)) {
            TokenStorage.saveToken(token);
            LOGGER.info("Stored new remember me token for user " + user.getEmail());
        } else {
            LOGGER.warning("Failed to store remember me token for user " + user.getEmail());
        }
    }

    private void clearRememberMe(int userId) {
        userRepository.clearRememberToken(userId);
        TokenStorage.deleteToken();
        LOGGER.info("Cleared remember me token for user " + userId);
    }

    private String generateNewToken() {
        byte[] bytes = new byte[64];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static class AuthenticationResult {
        private final boolean success;
        private final String  message;
        private final User    user;

        public AuthenticationResult(boolean success, String message, User user) {
            this.success = success;
            this.message = message;
            this.user    = user;
        }

        public boolean isSuccess() { return success; }
        public String  getMessage() { return message; }
        public User    getUser()    { return user; }
    }

    interface UserRepository {
        User authenticate(String email, char[] password);
        User findUserByRememberToken(String token);
        User findByEmail(String email);
        boolean updatePassword(String email, String newPassword);
        boolean storeRememberToken(int userId, String token, Timestamp expiry);
        boolean clearRememberToken(int userId);
    }

    private static final class UserDAOUserRepository implements UserRepository {
        private final UserDAO userDAO;

        private UserDAOUserRepository(UserDAO userDAO) {
            this.userDAO = userDAO;
        }

        @Override
        public User authenticate(String email, char[] password) {
            return userDAO.authenticate(email, password);
        }

        @Override
        public User findUserByRememberToken(String token) {
            return userDAO.findUserByRememberToken(token);
        }

        @Override
        public User findByEmail(String email) {
            return userDAO.findByEmail(email);
        }

        @Override
        public boolean updatePassword(String email, String newPassword) {
            return userDAO.updatePassword(email, newPassword);
        }

        @Override
        public boolean storeRememberToken(int userId, String token, Timestamp expiry) {
            return userDAO.storeRememberToken(userId, token, expiry);
        }

        @Override
        public boolean clearRememberToken(int userId) {
            return userDAO.clearRememberToken(userId);
        }
    }
}
