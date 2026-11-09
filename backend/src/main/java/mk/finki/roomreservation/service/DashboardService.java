package mk.finki.roomreservation.service;

import mk.finki.roomreservation.model.DashboardStats;
import mk.finki.roomreservation.model.HealthStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;

    public DashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public HealthStatus health() {
        Long roomCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project.rooms",
                Long.class
        );

        return new HealthStatus(
                "UP",
                "PostgreSQL",
                "project",
                roomCount == null ? 0 : roomCount
        );
    }

    public DashboardStats getDashboardStats() {
        return new DashboardStats(
                count("SELECT COUNT(*) FROM project.rooms"),
                count("SELECT COUNT(*) FROM project.equipment"),
                count("SELECT COUNT(*) FROM project.users"),
                count("SELECT COUNT(*) FROM project.reservations WHERE status = 'pending'"),
                count("SELECT COUNT(*) FROM project.reservations WHERE status = 'approved'")
        );
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }
}