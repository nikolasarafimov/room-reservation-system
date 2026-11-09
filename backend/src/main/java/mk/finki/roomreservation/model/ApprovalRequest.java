package mk.finki.roomreservation.model;

public record ApprovalRequest(
        int reservationId,
        int approverId,
        String decision,
        String note
) {
}