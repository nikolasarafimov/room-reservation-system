package mk.finki.roomreservation.model;

public record RoomUtilizationReport(
        String quarterStart,
        String buildingName,
        String roomCode,
        String type,
        int capacity,
        long totalRoomReservations,
        long approvedReservations,
        long rejectedReservations,
        long cancelledReservations,
        long pendingReservations,
        double requestedHours,
        double approvedHours,
        double approvedHoursSharePercent,
        int utilizationRank,
        String utilizationLevel
) {
}