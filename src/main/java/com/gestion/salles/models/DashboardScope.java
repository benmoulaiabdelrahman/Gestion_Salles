package com.gestion.salles.models;

public final class DashboardScope {
    public enum Role { ADMIN, CHEF_DEPARTEMENT, ENSEIGNANT }

    private final Role role;
    private final Integer blocId;

    private DashboardScope(Role role, Integer blocId) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null.");
        }
        this.role = role;
        this.blocId = blocId;
    }

    public static DashboardScope forAdmin() {
        return new DashboardScope(Role.ADMIN, null);
    }

    public static DashboardScope forChef(int blocId) {
        return new DashboardScope(Role.CHEF_DEPARTEMENT, blocId);
    }
    
    // For other roles, if needed, can add more factory methods
    // public static DashboardScope forEnseignant() { return new DashboardScope(Role.ENSEIGNANT, null); }

    public boolean isScoped() {
        return blocId != null;
    }

    public Integer getBlocId() {
        return blocId;
    }

    public Role getRole() {
        return role;
    }
}
