package mk.finki.roomreservation.service;

import mk.finki.roomreservation.model.ApprovalRequest;
import mk.finki.roomreservation.model.ApprovalResult;
import mk.finki.roomreservation.model.PendingReservation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApprovalService {

    private final JdbcTemplate jdbcTemplate;

    public ApprovalService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PendingReservation> findPendingReservations() {
        String sql = """
                SELECT
                    reservation_id,
                    requester_name,
                    reservation_date::text AS reservation_date,
                    start_time::text AS start_time,
                    end_time::text AS end_time,
                    COALESCE(room_code, 'No room') AS room_code,
                    COALESCE(building_name, 'No building') AS building_name,
                    status,
                    COALESCE(requested_equipment, 'No equipment') AS requested_equipment
                FROM project.v_pending_reservation_details
                ORDER BY reservation_date, start_time, reservation_id
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new PendingReservation(
                        rs.getInt("reservation_id"),
                        rs.getString("requester_name"),
                        rs.getString("reservation_date"),
                        rs.getString("start_time"),
                        rs.getString("end_time"),
                        rs.getString("room_code"),
                        rs.getString("building_name"),
                        rs.getString("status"),
                        rs.getString("requested_equipment")
                )
        );
    }

    public ApprovalResult approveOrReject(ApprovalRequest request) {
        validateApprovalRequest(request);

        String sql = """
                SELECT *
                FROM project.fn_approve_or_reject_reservation(
                    ?,
                    ?,
                    CAST(? AS project.approval_decision_domain),
                    ?
                )
                """;

        ApprovalResult result = jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> new ApprovalResult(
                        rs.getInt("out_approval_id"),
                        rs.getInt("out_reservation_id"),
                        rs.getString("out_status"),
                        rs.getString("out_decision"),
                        rs.getString("out_decision_time")
                ),
                request.reservationId(),
                request.approverId(),
                request.decision(),
                request.note()
        );

        if (result == null) {
            throw new RuntimeException("Approval operation failed.");
        }

        return result;
    }

    private void validateApprovalRequest(ApprovalRequest request) {
        if (request.reservationId() <= 0) {
            throw new IllegalArgumentException("Reservation ID is required.");
        }

        if (request.approverId() <= 0) {
            throw new IllegalArgumentException("Approver ID is required.");
        }

        if (request.decision() == null || request.decision().isBlank()) {
            throw new IllegalArgumentException("Decision is required.");
        }

        if (!request.decision().equals("approved") && !request.decision().equals("rejected")) {
            throw new IllegalArgumentException("Decision must be approved or rejected.");
        }
    }
}