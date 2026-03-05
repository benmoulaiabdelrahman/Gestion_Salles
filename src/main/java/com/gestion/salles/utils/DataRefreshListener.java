package com.gestion.salles.utils;

/******************************************************************************
 * DataRefreshListener.java
 *
 * Functional interface for UI components that need to react to underlying
 * data changes. Implement onDataChanged() to trigger a refresh.
 ******************************************************************************/

@FunctionalInterface
public interface DataRefreshListener {
    void onDataChanged();
}
