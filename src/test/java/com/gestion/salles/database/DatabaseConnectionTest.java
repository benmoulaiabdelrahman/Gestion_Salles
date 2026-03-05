package com.gestion.salles.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Database Connection Test Utility
 * Tests all aspects of database connectivity and schema
 * 
 * @author Your Name
 * @version 1.0
 */
public class DatabaseConnectionTest {
    
    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("  DATABASE CONNECTION TEST");
        System.out.println("==============================================\n");
        
        DatabaseConnection dbConnection = DatabaseConnection.getInstance();
        
        // Test 1: Basic Connection
        System.out.println("Test 1: Testing basic database connection...");
        if (testBasicConnection(dbConnection)) {
            System.out.println("✓ PASSED: Database connection successful!\n");
        } else {
            System.out.println("✗ FAILED: Unable to connect to database!\n");
            System.exit(1);
        }
        
        // Test 2: Database Schema
        System.out.println("Test 2: Checking database schema...");
        if (testDatabaseSchema(dbConnection)) {
            System.out.println("✓ PASSED: Database schema is properly initialized!\n");
        } else {
            System.out.println("✗ FAILED: Database schema is not initialized!\n");
            System.out.println("Please run gestion_salles.sql script first.\n");
            System.exit(1);
        }
        
        // Test 3: Table Counts
        System.out.println("Test 3: Verifying table data...");
        testTableData(dbConnection);
        
        // Test 4: Sample Query
        System.out.println("\nTest 4: Testing sample queries...");
        testSampleQueries(dbConnection);
        
        // Test 5: User Authentication Data
        System.out.println("\nTest 5: Checking user accounts...");
        testUserAccounts(dbConnection);
        
        System.out.println("\n==============================================");
        System.out.println("  ALL TESTS COMPLETED SUCCESSFULLY!");
        System.out.println("==============================================");
        System.out.println("\nYour database is ready to use.");
        System.out.println("You can now run the main application.\n");
        
    }
    
    /**
     * Test basic database connection
     */
    private static boolean testBasicConnection(DatabaseConnection dbConnection) {
        return dbConnection.verifyDatabaseAccess();
    }
    
    /**
     * Test if database schema is initialized
     */
    private static boolean testDatabaseSchema(DatabaseConnection dbConnection) {
        String[] requiredTables = {
            "departements", "utilisateurs", "niveaux", "blocs",
            "salles", "types_activites", "reservations", "historique_reservations",
            "activity_log", "audit_log", "active_sessions", "verification_codes"
        };
        
        try (Connection conn = dbConnection.getConnection()) {
            for (String table : requiredTables) {
                String query = "SELECT COUNT(*) FROM information_schema.tables " +
                             "WHERE table_schema = 'gestion_salles' AND table_name = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, table);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) == 0) {
                            System.out.println("  ✗ Missing table: " + table);
                            return false;
                        }
                    }
                }
            }
            
            System.out.println("  ✓ All required tables exist");
            return true;
            
        } catch (SQLException e) {
            System.err.println("  Error checking schema: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Test table data counts
     */
    private static void testTableData(DatabaseConnection dbConnection) {
        String[] tables = {
            "departements", "utilisateurs", "niveaux", 
            "blocs", "salles", "types_activites", "reservations"
        };
        
        try (Connection conn = dbConnection.getConnection()) {
            System.out.println("\n  Table Data Summary:");
            System.out.println("  " + "-".repeat(40));
            
            for (String table : tables) {
                String query = "SELECT COUNT(*) FROM " + table;
                try (PreparedStatement stmt = conn.prepareStatement(query);
                     ResultSet rs = stmt.executeQuery()) {
                    
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        System.out.printf("  %-25s: %d records%n", table, count);
                    }
                }
            }
            
            System.out.println("  " + "-".repeat(40));
            
        } catch (SQLException e) {
            System.err.println("  Error checking table data: " + e.getMessage());
        }
    }
    
    /**
     * Test sample queries
     */
    private static void testSampleQueries(DatabaseConnection dbConnection) {
        try (Connection conn = dbConnection.getConnection()) {
            
            // Test 1: Count departments
            String query1 = "SELECT COUNT(*) as total FROM departements WHERE actif = TRUE";
            try (PreparedStatement stmt = conn.prepareStatement(query1);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("  ✓ Active departments: " + rs.getInt("total"));
                }
            }
            
            // Test 2: Count users by role
            String query2 = "SELECT role, COUNT(*) as total FROM utilisateurs " +
                          "WHERE actif = TRUE GROUP BY role";
            try (PreparedStatement stmt = conn.prepareStatement(query2);
                 ResultSet rs = stmt.executeQuery()) {
                System.out.println("  ✓ Users by role:");
                while (rs.next()) {
                    System.out.printf("    - %s: %d users%n", 
                        rs.getString("role"), rs.getInt("total"));
                }
            }
            
            // Test 3: Count available rooms
            String query3 = "SELECT COUNT(*) as total FROM salles WHERE actif = TRUE";
            try (PreparedStatement stmt = conn.prepareStatement(query3);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("  ✓ Available rooms: " + rs.getInt("total"));
                }
            }
            
            // Test 4: Check pending reservations
            String query4 = "SELECT COUNT(*) as total FROM reservations " +
                          "WHERE statut = 'EN_ATTENTE'";
            try (PreparedStatement stmt = conn.prepareStatement(query4);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int pendingReservations = rs.getInt("total");
                    System.out.println("  ✓ Pending reservations: " + pendingReservations);
                    if (pendingReservations > 0) {
                        System.out.println("    ⚠ Warning: There are pending reservations to review.");
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("  Error executing sample queries: " + e.getMessage());
        }
    }
    
    /**
     * Test user accounts and display login credentials
     */
    private static void testUserAccounts(DatabaseConnection dbConnection) {
        String query = "SELECT email, role, CONCAT(prenom, ' ', nom) as nom_complet " +
                      "FROM utilisateurs WHERE actif = TRUE ORDER BY role, nom";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            System.out.println("\n  Available Test Accounts:");
            System.out.println("  " + "=".repeat(60));
            System.out.println("  Note: passwords are not displayed by this utility.");
            System.out.println("  " + "-".repeat(60));
            
            String currentRole = "";
            while (rs.next()) {
                String role = rs.getString("role");
                if (!role.equals(currentRole)) {
                    currentRole = role;
                    System.out.println("\n  " + role + ":");
                }
                System.out.printf("    %-30s - %s%n", 
                    rs.getString("email"), 
                    rs.getString("nom_complet"));
            }
            
            System.out.println("  " + "=".repeat(60));
            System.out.println("\n  ✓ Active accounts listed successfully.");
            
        } catch (SQLException e) {
            System.err.println("  Error checking user accounts: " + e.getMessage());
        }
    }
}
