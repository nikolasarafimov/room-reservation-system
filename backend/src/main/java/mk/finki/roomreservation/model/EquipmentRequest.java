package mk.finki.roomreservation.model;

public record EquipmentRequest(
        int equipmentId,
        int requestedQuantity
) {
}