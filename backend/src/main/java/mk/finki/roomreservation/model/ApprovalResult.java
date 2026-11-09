package mk.finki.roomreservation.model;

public record ApprovalResult(
        int approvalId,
        int reservationId,
        String status,
        String decision,
        String decisionTime
) {
}