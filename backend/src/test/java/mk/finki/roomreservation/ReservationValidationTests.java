package mk.finki.roomreservation;

import mk.finki.roomreservation.model.CreateReservationRequest;
import mk.finki.roomreservation.model.EquipmentRequest;
import mk.finki.roomreservation.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.format.DateTimeParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ReservationValidationTests {

    private JdbcTemplate jdbcTemplate;
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        reservationService = new ReservationService(jdbcTemplate);
    }

    @Test
    void rejectsMissingReservationDate() {
        assertInvalid(
                request("", "10:00", "12:00", 1, List.of()),
                IllegalArgumentException.class
        );
    }

    @Test
    void rejectsMalformedReservationDate() {
        assertInvalid(
                request("invalid-date", "10:00", "12:00", 1, List.of()),
                DateTimeParseException.class
        );
    }

    @Test
    void rejectsInvalidStartTime() {
        assertInvalid(
                request("2030-01-20", "25:00", "12:00", 1, List.of()),
                DateTimeParseException.class
        );
    }

    @Test
    void rejectsIdenticalStartAndEndTimes() {
        assertInvalid(
                request("2030-01-20", "10:00", "10:00", 1, List.of()),
                IllegalArgumentException.class
        );
    }

    @Test
    void rejectsEndTimeBeforeStartTime() {
        assertInvalid(
                request("2030-01-20", "14:00", "12:00", 1, List.of()),
                IllegalArgumentException.class
        );
    }

    @Test
    void rejectsReservationWithoutResources() {
        assertInvalid(
                request("2030-01-20", "10:00", "12:00", null, List.of()),
                IllegalArgumentException.class
        );
    }

    @Test
    void rejectsInvalidEquipmentId() {
        assertInvalid(
                request(
                        "2030-01-20",
                        "10:00",
                        "12:00",
                        null,
                        List.of(new EquipmentRequest(0, 2))
                ),
                IllegalArgumentException.class
        );
    }

    @Test
    void rejectsNonPositiveEquipmentQuantity() {
        assertInvalid(
                request(
                        "2030-01-20",
                        "10:00",
                        "12:00",
                        1,
                        List.of(new EquipmentRequest(3, 0))
                ),
                IllegalArgumentException.class
        );
    }

    @Test
    void rejectsPastReservationDate() {
        String yesterday = java.time.LocalDate
                .now(java.time.ZoneId.of("Europe/Skopje"))
                .minusDays(1)
                .toString();

        assertInvalid(
                request(yesterday, "10:00", "12:00", 1, List.of()),
                IllegalArgumentException.class
        );
    }

    @Test
    void rejectsInvalidRoomId() {
        assertInvalid(
                request("2030-01-20", "10:00", "12:00", -1, List.of()),
                IllegalArgumentException.class
        );
    }

    @Test
    void rejectsInvalidUserId() {
        CreateReservationRequest invalidRequest =
                new CreateReservationRequest(
                        0,
                        "2030-01-20",
                        "10:00",
                        "12:00",
                        1,
                        List.of()
                );

        assertInvalid(invalidRequest, IllegalArgumentException.class);
    }

    private CreateReservationRequest request(
            String date,
            String startTime,
            String endTime,
            Integer roomId,
            List<EquipmentRequest> equipment
    ) {
        return new CreateReservationRequest(
                1,
                date,
                startTime,
                endTime,
                roomId,
                equipment
        );
    }

    private void assertInvalid(
            CreateReservationRequest request,
            Class<? extends RuntimeException> expectedException
    ) {
        assertThrows(
                expectedException,
                () -> reservationService.createReservation(request)
        );

        verifyNoInteractions(jdbcTemplate);
    }
}