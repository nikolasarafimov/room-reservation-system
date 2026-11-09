package mk.finki.roomreservation.service;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseUserDetailsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            return jdbcTemplate.queryForObject("""
                SELECT username, password_hash, enabled, role
                FROM project.users
                WHERE username = ?
                """, (rs, rowNum) -> {
                    String hash = rs.getString("password_hash");
                    if (hash == null || hash.isBlank()) {
                        throw new UsernameNotFoundException("Account has no password");
                    }
                    return User.withUsername(rs.getString("username"))
                        .password(hash)
                        .roles(rs.getString("role").toUpperCase(Locale.ROOT))
                        .disabled(!rs.getBoolean("enabled"))
                        .build();
                }, username);
        } catch (EmptyResultDataAccessException ex) {
            throw new UsernameNotFoundException("Account not found", ex);
        }
    }
}
