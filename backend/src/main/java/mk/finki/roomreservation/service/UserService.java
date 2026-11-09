package mk.finki.roomreservation.service;

import mk.finki.roomreservation.model.UserOption;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final JdbcTemplate jdbcTemplate;

    public UserService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<UserOption> findAllUsers() {
        String sql = """
                SELECT
                    user_id,
                    username,
                    email,
                    full_name,
                    role
                FROM project.users
                ORDER BY user_id
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new UserOption(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("full_name"),
                        rs.getString("role")
                )
        );
    }

    public UserOption findByUsername(String username) {
        String sql = """
                SELECT user_id, username, email, full_name, role
                FROM project.users
                WHERE username = ? AND enabled = TRUE
                """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                new UserOption(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("full_name"),
                        rs.getString("role")
                ), username);
    }

    public boolean ownsReservation(int reservationId, int userId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM project.reservations
                WHERE reservation_id = ? AND user_id = ?
                """, Integer.class, reservationId, userId);
        return count != null && count > 0;
    }

    public List<UserOption> findApprovers() {
        String sql = """
                SELECT
                    user_id,
                    username,
                    email,
                    full_name,
                    role
                FROM project.users
                WHERE role IN ('admin', 'approver')
                ORDER BY user_id
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new UserOption(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("full_name"),
                        rs.getString("role")
                )
        );
    }
}