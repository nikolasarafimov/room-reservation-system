package mk.finki.roomreservation.model;

public record SearchRoomsRequest(
        String reservationDate,
        String startTime,
        String endTime,
        Integer minimumCapacity,
        String roomType,
        String requiredEquipmentName
) {
}