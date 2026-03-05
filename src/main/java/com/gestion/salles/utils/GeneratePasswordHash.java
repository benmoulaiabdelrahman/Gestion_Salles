package com.gestion.salles.utils;

/******************************************************************************
 * GeneratePasswordHash.java
 *
 * Developer utility for generating BCrypt password hashes from the command
 * line. Accepts a plain-text password as a CLI argument, prints the resulting
 * BCrypt hash, verifies it, and outputs a ready-to-use SQL UPDATE statement.
 *
 * Usage:
 *   java GeneratePasswordHash <password>
 ******************************************************************************/

import org.mindrot.jbcrypt.BCrypt;

public class GeneratePasswordHash {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java GeneratePasswordHash <password>");
            System.exit(1);
        }

        String password = args[0];
        String hash     = BCrypt.hashpw(password, BCrypt.gensalt(10));

        System.out.println("=== BCrypt Password Hash Generator ===\n");
        System.out.println("Password  : " + password);
        System.out.println("BCrypt Hash: " + hash);
        System.out.println("Verified  : " + BCrypt.checkpw(password, hash));

        System.out.println("\n=== SQL Update Statement ===");
        System.out.println("UPDATE utilisateurs SET mot_de_passe = '" + hash + "';");
        System.out.println("UPDATE utilisateurs SET mot_de_passe = '" + hash + "' WHERE email = 'your@email.com';");
    }
}
