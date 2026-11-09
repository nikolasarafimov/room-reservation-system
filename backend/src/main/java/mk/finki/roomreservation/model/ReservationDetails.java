package mk.finki.roomreservation.model;

public record ReservationDetails(
        int reservationId,
        String requesterName,
        String roomCode,
        String reservationDate,
        String startTime,
        String endTime,
        String status,
        String equipment
) {
}