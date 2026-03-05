package com.gestion.salles.models;

/******************************************************************************
 * ActivityType.java
 *
 * Model for the 'types_activites' table. colorHex is stored as a "#RRGGBB"
 * hex string; getColor() converts it to java.awt.Color for UI use, falling
 * back to Color.GRAY on null or invalid input. Equality and hashing are
 * based solely on id.
 ******************************************************************************/

import java.awt.Color;
import java.util.Objects;

public class ActivityType {

    private int id;
    private String name;
    private String colorHex;
    private boolean isGroupSpecific;

    public ActivityType() {}

    public ActivityType(String name) {
        this.name = name;
    }

    public ActivityType(String name, int id) {
        this.id = id;
        this.name = name;
    }

    public ActivityType(int id, String name, String colorHex, boolean isGroupSpecific) {
        this.id = id;
        this.name = name;
        this.colorHex = colorHex;
        this.isGroupSpecific = isGroupSpecific;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public boolean isGroupSpecific() { return isGroupSpecific; }
    public void setGroupSpecific(boolean groupSpecific) { this.isGroupSpecific = groupSpecific; }

    public Color getColor() {
        if (colorHex != null && colorHex.matches("^#([A-Fa-f0-9]{6})$")) {
            return Color.decode(colorHex);
        }
        return Color.GRAY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return id == ((ActivityType) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return name; }
}
