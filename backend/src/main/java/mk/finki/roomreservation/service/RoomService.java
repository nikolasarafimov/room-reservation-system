package mk.finki.roomreservation.service;

import mk.finki.roomreservation.model.RoomOption;
import mk.finki.roomreservation.model.SearchRoomsRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class RoomService {

    private final JdbcTemplate jdbcTemplate;

    public RoomService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RoomOption> findAllRooms() {
        String sql = """
                SELECT
                    r.room_id,
                    r.room_code,
                    r.capacity,
                    r.type,
                    b.name AS building_name,
                    b.address
                FROM project.rooms r
                JOIN project.buildings b
                    ON r.building_id = b.building_id
                ORDER BY b.name, r.room_code
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new RoomOption(
                        rs.getInt("room_id"),
                        rs.getString("room_code"),
                        rs.getInt("capacity"),
                        rs.getString("type"),
                        rs.getString("building_name"),
                        rs.getString("address")
                )
        );
    }

    public List<RoomOption> searchAvailableRooms(SearchRoomsRequest request) {
        validateSearchRequest(request);

        LocalDate date = LocalDate.parse(request.reservationDate());
        LocalTime start = LocalTime.parse(request.startTime());
        LocalTime end = LocalTime.parse(request.endTime());

        int minimumCapacity = request.minimumCapacity() == null ? 1 : request.minimumCapacity();
        String roomType = normalizeBlank(request.roomType());
        String equipmentName = normalizeBlank(request.requiredEquipmentName());

        String sql = """
                SELECT
                    r.room_id,
                    r.room_code,
                    r.capacity,
                    r.type,
                    b.name AS building_name,
                    b.address
                FROM project.rooms r
                JOIN project.buildings b
                    ON r.building_id = b.building_id
                WHERE r.capacity >= ?
                  AND (? = '' OR r.type = ?)
                  AND (
                        ? = ''
                        OR EXISTS (
                            SELECT 1
                            FROM project.room_equipment re
                            JOIN project.equipment e
                                ON re.equipment_id = e.equipment_id
                            WHERE re.room_id = r.room_id
                              AND LOWER(e.name) LIKE LOWER('%' || ? || '%')
                        )
                  )
                  AND NOT EXISTS (
                        SELECT 1
                        FROM project.reservations res
                        WHERE res.room_id = r.room_id
                          AND res.reservation_date = ?
                          AND res.status IN ('pending', 'approved')
                          AND ? < res.end_time
                          AND ? > res.start_time
                  )
                ORDER BY r.capacity, r.room_code
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new RoomOption(
                        rs.getInt("room_id"),
                        rs.getString("room_code"),
                        rs.getInt("capacity"),
                        rs.getString("type"),
                        rs.getString("building_name"),
                        rs.getString("address")
                ),
                minimumCapacity,
                roomType,
                roomType,
                equipmentName,
                equipmentName,
                Date.valueOf(date),
                Time.valueOf(start),
                Time.valueOf(end)
        );
    }

    private void validateSearchRequest(SearchRoomsRequest request) {
        if (request.reservationDate() == null || request.reservationDate().isBlank()) {
            throw new IllegalArgumentException("Reservation date is required.");
        }

        if (request.startTime() == null || request.startTime().isBlank()) {
            throw new IllegalArgumentException("Start time is required.");
        }

        if (request.endTime() == null || request.endTime().isBlank()) {
            throw new IllegalArgumentException("End time is required.");
        }

        LocalTime start = LocalTime.parse(request.startTime());
        LocalTime end = LocalTime.parse(request.endTime());

        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }

        if (request.minimumCapacity() != null && request.minimumCapacity() < 1) {
            throw new IllegalArgumentException("Minimum capacity must be greater than 0.");
        }
    }

    private String normalizeBlank(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }

        return value.trim();
    }
}