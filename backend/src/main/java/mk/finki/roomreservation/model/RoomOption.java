package mk.finki.roomreservation.model;

public record RoomOption(
        int roomId,
        String roomCode,
        int capacity,
        String type,
        String buildingName,
        String address
) {
}