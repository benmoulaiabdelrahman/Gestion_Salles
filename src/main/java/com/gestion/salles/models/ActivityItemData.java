package com.gestion.salles.models;

import javax.swing.ImageIcon;

public class ActivityItemData {
    private final ActivityLog activityLog;
    private final ImageIcon resolvedIcon;

    public ActivityItemData(ActivityLog activityLog, ImageIcon resolvedIcon) {
        this.activityLog = activityLog;
        this.resolvedIcon = resolvedIcon;
    }

    public ActivityLog getActivityLog() {
        return activityLog;
    }

    public ImageIcon getResolvedIcon() {
        return resolvedIcon;
    }
}
