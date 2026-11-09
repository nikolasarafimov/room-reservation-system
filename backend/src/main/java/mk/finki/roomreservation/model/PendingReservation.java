package mk.finki.roomreservation.model;

public record PendingReservation(
        int reservationId,
        String requesterName,
        String reservationDate,
        String startTime,
        String endTime,
        String roomCode,
        String buildingName,
        String status,
        String requestedEquipment
) {
}