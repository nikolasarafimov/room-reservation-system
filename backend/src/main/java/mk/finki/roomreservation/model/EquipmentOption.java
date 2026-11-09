package mk.finki.roomreservation.model;

public record EquipmentOption(
        int equipmentId,
        String name,
        int stockQuantity
) {
}