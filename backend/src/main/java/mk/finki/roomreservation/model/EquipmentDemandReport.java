package mk.finki.roomreservation.model;

public record EquipmentDemandReport(
        String quarterStart,
        String equipmentName,
        int stockQuantity,
        int assignedRoomQuantity,
        int totalRegisteredQuantity,
        long reservationsWithEquipment,
        int totalRequestedQuantity,
        int approvedRequestedQuantity,
        int pendingRequestedQuantity,
        int rejectedRequestedQuantity,
        double demandToRegisteredPercent,
        int demandRank,
        String demandLevel
) {
}