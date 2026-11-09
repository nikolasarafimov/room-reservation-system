package mk.finki.roomreservation;

import mk.finki.roomreservation.model.SearchRoomsRequest;
import mk.finki.roomreservation.service.DashboardService;
import mk.finki.roomreservation.service.ReportService;
import mk.finki.roomreservation.service.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        RoomService.class,
        DashboardService.class,
        ReportService.class
})
@Testcontainers
class PostgreSqlIntegrationTests {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("room_reservation")
                    .withUsername("roomreservation")
                    .withPassword("test_only_password")
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource("db/init/01_schema.sql"),
                            "/docker-entrypoint-initdb.d/01_schema.sql"
                    )
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource("db/init/02_seed.sql"),
                            "/docker-entrypoint-initdb.d/02_seed.sql"
                    )
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource("db/init/03_advanced.sql"),
                            "/docker-entrypoint-initdb.d/03_advanced.sql"
                    )
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource("db/init/05_auth_schema.sql"),
                            "/docker-entrypoint-initdb.d/05_auth_schema.sql"
                    );

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RoomService roomService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private ReportService reportService;

    @Test
    void connectsToIsolatedPostgreSql() {
        String database = jdbcTemplate.queryForObject(
                "SELECT current_database()",
                String.class
        );

        assertThat(database).isEqualTo("room_reservation");

        Integer schemaCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_namespace WHERE nspname = 'project'",
                Integer.class
        );

        assertThat(schemaCount).isEqualTo(1);
    }

    @Test
    void roomServiceAndDashboardReadRealData() {
        var rooms = roomService.findAllRooms();
        var dashboard = dashboardService.getDashboardStats();

        assertThat(rooms).hasSize(5);
        assertThat(dashboard.totalRooms()).isEqualTo(5);
        assertThat(dashboard.totalEquipmentTypes()).isEqualTo(7);
        assertThat(dashboard.totalUsers()).isEqualTo(5);
    }

    @Test
    void searchExcludesAlreadyReservedRoom() {
        BookedRoom booked = jdbcTemplate.queryForObject("""
                SELECT room_id, reservation_date, start_time, end_time
                FROM project.reservations
                WHERE room_id IS NOT NULL
                  AND status IN ('pending', 'approved')
                ORDER BY reservation_id
                LIMIT 1
                """, (rs, rowNum) -> new BookedRoom(
                rs.getInt("room_id"),
                rs.getDate("reservation_date").toLocalDate(),
                rs.getTime("start_time").toLocalTime(),
                rs.getTime("end_time").toLocalTime()
        ));

        assertThat(booked).isNotNull();

        var availableRooms = roomService.searchAvailableRooms(
                new SearchRoomsRequest(
                        booked.date().toString(),
                        booked.startTime().toString(),
                        booked.endTime().toString(),
                        1,
                        "",
                        ""
                )
        );

        assertThat(availableRooms)
                .noneMatch(room -> room.roomId() == booked.roomId());
    }

    @Test
    void reportsReadPostgreSqlData() {
        var roomUtilization = reportService.roomUtilizationReport();
        var equipmentDemand = reportService.equipmentDemandReport();

        assertThat(roomUtilization).isNotEmpty();
        assertThat(equipmentDemand).isNotEmpty();
    }

    private record BookedRoom(
            int roomId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {
    }
}