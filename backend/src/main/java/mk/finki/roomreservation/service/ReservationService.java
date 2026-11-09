package mk.finki.roomreservation.service;

import mk.finki.roomreservation.model.CreateReservationRequest;
import mk.finki.roomreservation.model.EquipmentRequest;
import mk.finki.roomreservation.model.ReservationDetails;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.time.ZoneId;

@Service
public class ReservationService {

    private final JdbcTemplate jdbcTemplate;

    public ReservationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ReservationDetails createReservation(CreateReservationRequest request) {
        validateCreateRequest(request);

        LocalDate date = LocalDate.parse(request.reservationDate());
        LocalTime start = LocalTime.parse(request.startTime());
        LocalTime end = LocalTime.parse(request.endTime());

        List<EquipmentRequest> equipment =
                request.equipment() == null ? List.of() : request.equipment();

        String insertReservationSql = """
                INSERT INTO project.reservations (
                    room_id,
                    user_id,
                    reservation_date,
                    start_time,
                    end_time,
                    status
                )
                VALUES (?, ?, ?, ?, ?, 'pending')
                RETURNING reservation_id
                """;

        Integer reservationId = jdbcTemplate.queryForObject(
                insertReservationSql,
                Integer.class,
                request.roomId(),
                request.userId(),
                Date.valueOf(date),
                Time.valueOf(start),
                Time.valueOf(end)
        );

        if (reservationId == null) {
            throw new RuntimeException("Reservation was not created.");
        }

        for (EquipmentRequest item : equipment) {
            jdbcTemplate.update(
                    """
                    INSERT INTO project.reservation_equipment (
                        reservation_id,
                        equipment_id,
                        requested_quantity
                    )
                    VALUES (?, ?, ?)
                    """,
                    reservationId,
                    item.equipmentId(),
                    item.requestedQuantity()
            );
        }

        return findReservationDetails(reservationId);
    }

    public ReservationDetails findReservationDetails(int reservationId) {
        String sql = """
                SELECT
                    res.reservation_id,
                    u.full_name AS requester_name,
                    COALESCE(r.room_code, 'No room') AS room_code,
                    res.reservation_date::text AS reservation_date,
                    res.start_time::text AS start_time,
                    res.end_time::text AS end_time,
                    res.status,
                    COALESCE(eq.requested_equipment, 'No equipment') AS equipment
                FROM project.reservations res
                JOIN project.users u
                    ON res.user_id = u.user_id
                LEFT JOIN project.rooms r
                    ON res.room_id = r.room_id
                LEFT JOIN (
                    SELECT
                        req.reservation_id,
                        string_agg(e.name || ' x' || req.requested_quantity, ', ' ORDER BY e.name) AS requested_equipment
                    FROM project.reservation_equipment req
                    JOIN project.equipment e
                        ON req.equipment_id = e.equipment_id
                    GROUP BY req.reservation_id
                ) eq
                    ON res.reservation_id = eq.reservation_id
                WHERE res.reservation_id = ?
                """;

        ReservationDetails details = jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> new ReservationDetails(
                        rs.getInt("reservation_id"),
                        rs.getString("requester_name"),
                        rs.getString("room_code"),
                        rs.getString("reservation_date"),
                        rs.getString("start_time"),
                        rs.getString("end_time"),
                        rs.getString("status"),
                        rs.getString("equipment")
                ),
                reservationId
        );

        if (details == null) {
            throw new RuntimeException("Reservation not found.");
        }

        return details;
    }

    private void validateCreateRequest(CreateReservationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Reservation request is required.");
        }

        if (request.userId() == null || request.userId() <= 0) {
            throw new IllegalArgumentException("Valid requester user ID is required.");
        }

        if (request.reservationDate() == null || request.reservationDate().isBlank()) {
            throw new IllegalArgumentException("Reservation date is required.");
        }

        LocalDate date = LocalDate.parse(request.reservationDate());
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Skopje"));

        if (date.isBefore(today)) {
            throw new IllegalArgumentException("Reservation date cannot be in the past.");
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

        if (request.roomId() != null && request.roomId() <= 0) {
            throw new IllegalArgumentException("Room ID must be greater than 0.");
        }

        List<EquipmentRequest> equipment =
                request.equipment() == null ? List.of() : request.equipment();

        if (request.roomId() == null && equipment.isEmpty()) {
            throw new IllegalArgumentException(
                    "Reservation must include at least one resource: room or equipment."
            );
        }

        for (EquipmentRequest item : equipment) {
            if (item == null) {
                throw new IllegalArgumentException("Equipment request cannot be null.");
            }

            if (item.equipmentId() <= 0) {
                throw new IllegalArgumentException("Equipment ID must be greater than 0.");
            }

            if (item.requestedQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Requested equipment quantity must be greater than 0."
                );
            }
        }
    }
}