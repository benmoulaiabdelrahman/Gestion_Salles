package com.gestion.salles.utils;

import com.gestion.salles.database.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class SeededUserPasswordSeeder {

    private SeededUserPasswordSeeder() {
    }

    public static void main(String[] args) throws SQLException {
        String selectSql = "SELECT id_utilisateur, email FROM utilisateurs " +
                           "WHERE id_utilisateur BETWEEN 197 AND 627 ORDER BY id_utilisateur";
        String updateSql = "UPDATE utilisateurs SET mot_de_passe = ?, must_change_password = TRUE WHERE id_utilisateur = ?";

        Set<String> generatedPasswords = new HashSet<>();
        int updatedCount = 0;

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql);
             PreparedStatement updateStmt = conn.prepareStatement(updateSql);
             ResultSet rs = selectStmt.executeQuery()) {

            while (rs.next()) {
                int userId = rs.getInt("id_utilisateur");
                String email = rs.getString("email");

                String plainPassword = generateUniquePassword(generatedPasswords);
                String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));

                updateStmt.setString(1, hash);
                updateStmt.setInt(2, userId);
                updateStmt.addBatch();

                // Export these lines to a secure file/channel for one-time delivery to users.
                System.out.println(userId + "," + email + "," + plainPassword);
                updatedCount++;
            }

            updateStmt.executeBatch();
        }

        System.out.println("Updated seeded users: " + updatedCount);
    }

    private static String generateUniquePassword(Set<String> generatedPasswords) {
        String password;
        do {
            password = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        } while (!generatedPasswords.add(password));
        return password;
    }
}
