package mk.finki.roomreservation.controller;

import mk.finki.roomreservation.model.ApprovalRequest;
import mk.finki.roomreservation.model.ApprovalResult;
import mk.finki.roomreservation.model.PendingReservation;
import mk.finki.roomreservation.model.UserOption;
import mk.finki.roomreservation.service.ApprovalService;
import mk.finki.roomreservation.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ApprovalController {

    private final ApprovalService approvalService;
    private final UserService userService;

    public ApprovalController(ApprovalService approvalService, UserService userService) {
        this.approvalService = approvalService;
        this.userService = userService;
    }

    @GetMapping("/api/reservations/pending")
    public List<PendingReservation> findPendingReservations() {
        return approvalService.findPendingReservations();
    }

    @PostMapping("/api/approvals")
    public ApprovalResult approveOrReject(@RequestBody ApprovalRequest request,
                                          Authentication authentication) {
        UserOption approver = userService.findByUsername(authentication.getName());
        ApprovalRequest trusted = new ApprovalRequest(
                request.reservationId(), approver.userId(), request.decision(), request.note());
        return approvalService.approveOrReject(trusted);
    }
}
