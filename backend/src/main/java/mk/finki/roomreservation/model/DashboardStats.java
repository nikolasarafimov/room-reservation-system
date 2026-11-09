package mk.finki.roomreservation.model;

public record DashboardStats(
        long totalRooms,
        long totalEquipmentTypes,
        long totalUsers,
        long pendingReservations,
        long approvedReservations
) {
}