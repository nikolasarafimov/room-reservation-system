package mk.finki.roomreservation.service;

import mk.finki.roomreservation.model.EquipmentOption;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EquipmentService {

    private final JdbcTemplate jdbcTemplate;

    public EquipmentService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<EquipmentOption> findAllEquipment() {
        String sql = """
                SELECT
                    equipment_id,
                    name,
                    stock_quantity
                FROM project.equipment
                ORDER BY name
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new EquipmentOption(
                        rs.getInt("equipment_id"),
                        rs.getString("name"),
                        rs.getInt("stock_quantity")
                )
        );
    }
}