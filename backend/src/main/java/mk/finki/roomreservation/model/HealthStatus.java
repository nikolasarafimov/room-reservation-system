package mk.finki.roomreservation.model;

public record HealthStatus(
        String status,
        String database,
        String schema,
        long roomCount
) {
}