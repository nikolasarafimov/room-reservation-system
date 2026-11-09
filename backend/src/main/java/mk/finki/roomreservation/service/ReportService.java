package mk.finki.roomreservation.service;

import mk.finki.roomreservation.model.EquipmentDemandReport;
import mk.finki.roomreservation.model.RoomUtilizationReport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportService {

    private final JdbcTemplate jdbcTemplate;

    public ReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RoomUtilizationReport> roomUtilizationReport() {
        String sql = """
                SELECT
                    quarter_start::text AS quarter_start,
                    building_name,
                    room_code,
                    type,
                    capacity,
                    total_room_reservations,
                    approved_reservations,
                    rejected_reservations,
                    cancelled_reservations,
                    pending_reservations,
                    requested_hours,
                    approved_hours,
                    approved_hours_share_percent,
                    utilization_rank,
                    utilization_level
                FROM project.v_quarterly_room_utilization
                ORDER BY quarter_start, utilization_rank, building_name, room_code
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new RoomUtilizationReport(
                        rs.getString("quarter_start"),
                        rs.getString("building_name"),
                        rs.getString("room_code"),
                        rs.getString("type"),
                        rs.getInt("capacity"),
                        rs.getLong("total_room_reservations"),
                        rs.getLong("approved_reservations"),
                        rs.getLong("rejected_reservations"),
                        rs.getLong("cancelled_reservations"),
                        rs.getLong("pending_reservations"),
                        rs.getDouble("requested_hours"),
                        rs.getDouble("approved_hours"),
                        rs.getDouble("approved_hours_share_percent"),
                        rs.getInt("utilization_rank"),
                        rs.getString("utilization_level")
                )
        );
    }

    public List<EquipmentDemandReport> equipmentDemandReport() {
        String sql = """
                WITH room_stock AS (
                    SELECT
                        re.equipment_id,
                        SUM(re.quantity) AS assigned_room_quantity
                    FROM project.room_equipment re
                    GROUP BY re.equipment_id
                ),
                equipment_demand AS (
                    SELECT
                        date_trunc('quarter', res.reservation_date)::date AS quarter_start,
                        e.equipment_id,
                        e.name AS equipment_name,
                        e.stock_quantity,
                        COALESCE(rs.assigned_room_quantity, 0) AS assigned_room_quantity,
                        e.stock_quantity + COALESCE(rs.assigned_room_quantity, 0) AS total_registered_quantity,
                        COUNT(DISTINCT req.reservation_id) AS reservations_with_equipment,
                        SUM(req.requested_quantity) AS total_requested_quantity,
                        COALESCE(SUM(req.requested_quantity) FILTER (WHERE res.status = 'approved'), 0) AS approved_requested_quantity,
                        COALESCE(SUM(req.requested_quantity) FILTER (WHERE res.status = 'pending'), 0) AS pending_requested_quantity,
                        COALESCE(SUM(req.requested_quantity) FILTER (WHERE res.status = 'rejected'), 0) AS rejected_requested_quantity
                    FROM project.reservation_equipment req
                    JOIN project.reservations res
                        ON req.reservation_id = res.reservation_id
                    JOIN project.equipment e
                        ON req.equipment_id = e.equipment_id
                    LEFT JOIN room_stock rs
                        ON e.equipment_id = rs.equipment_id
                    GROUP BY
                        date_trunc('quarter', res.reservation_date)::date,
                        e.equipment_id,
                        e.name,
                        e.stock_quantity,
                        rs.assigned_room_quantity
                )
                SELECT
                    quarter_start::text AS quarter_start,
                    equipment_name,
                    stock_quantity,
                    assigned_room_quantity,
                    total_registered_quantity,
                    reservations_with_equipment,
                    total_requested_quantity,
                    approved_requested_quantity,
                    pending_requested_quantity,
                    rejected_requested_quantity,
                    ROUND(
                        total_requested_quantity::numeric / NULLIF(total_registered_quantity, 0) * 100,
                        2
                    ) AS demand_to_registered_percent,
                    DENSE_RANK() OVER (
                        PARTITION BY quarter_start
                        ORDER BY total_requested_quantity DESC, reservations_with_equipment DESC, equipment_name
                    ) AS demand_rank,
                    CASE
                        WHEN total_requested_quantity > total_registered_quantity THEN 'stock_risk'
                        WHEN total_requested_quantity >= total_registered_quantity * 0.75 THEN 'high_demand'
                        WHEN total_requested_quantity > 0 THEN 'normal_demand'
                        ELSE 'no_demand'
                    END AS demand_level
                FROM equipment_demand
                ORDER BY quarter_start, demand_rank, equipment_name
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new EquipmentDemandReport(
                        rs.getString("quarter_start"),
                        rs.getString("equipment_name"),
                        rs.getInt("stock_quantity"),
                        rs.getInt("assigned_room_quantity"),
                        rs.getInt("total_registered_quantity"),
                        rs.getLong("reservations_with_equipment"),
                        rs.getInt("total_requested_quantity"),
                        rs.getInt("approved_requested_quantity"),
                        rs.getInt("pending_requested_quantity"),
                        rs.getInt("rejected_requested_quantity"),
                        rs.getDouble("demand_to_registered_percent"),
                        rs.getInt("demand_rank"),
                        rs.getString("demand_level")
                )
        );
    }
}