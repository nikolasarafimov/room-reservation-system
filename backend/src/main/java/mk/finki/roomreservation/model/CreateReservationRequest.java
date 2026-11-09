package mk.finki.roomreservation.model;

import java.util.List;

public record CreateReservationRequest(
        Integer userId,
        String reservationDate,
        String startTime,
        String endTime,
        Integer roomId,
        List<EquipmentRequest> equipment
) {
}