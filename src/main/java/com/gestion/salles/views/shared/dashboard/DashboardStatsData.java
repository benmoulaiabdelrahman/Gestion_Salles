package com.gestion.salles.views.shared.dashboard;

import java.util.Map;

public class DashboardStatsData {
    private final boolean empty;
    private final Integer roomCount;
    private final Integer userCount;
    private final Integer reservationsToday;
    private final Integer roomsInUse;
    private final Map<String, Integer> roomTypeDistribution;

    private DashboardStatsData(boolean empty,
                               Integer roomCount,
                               Integer userCount,
                               Integer reservationsToday,
                               Integer roomsInUse,
                               Map<String, Integer> roomTypeDistribution) {
        this.empty = empty;
        this.roomCount = roomCount;
        this.userCount = userCount;
        this.reservationsToday = reservationsToday;
        this.roomsInUse = roomsInUse;
        this.roomTypeDistribution = roomTypeDistribution;
    }

    public static DashboardStatsData empty() {
        return new DashboardStatsData(true, null, null, null, null, null);
    }

    public static DashboardStatsData of(Integer roomCount,
                                        Integer userCount,
                                        Integer reservationsToday,
                                        Integer roomsInUse,
                                        Map<String, Integer> roomTypeDistribution) {
        return new DashboardStatsData(false, roomCount, userCount, reservationsToday, roomsInUse, roomTypeDistribution);
    }

    public boolean isEmpty() { return empty; }

    public Integer getRoomCount() { return roomCount; }

    public Integer getUserCount() { return userCount; }

    public Integer getReservationsToday() { return reservationsToday; }

    public Integer getRoomsInUse() { return roomsInUse; }

    public Map<String, Integer> getRoomTypeDistribution() { return roomTypeDistribution; }
}
