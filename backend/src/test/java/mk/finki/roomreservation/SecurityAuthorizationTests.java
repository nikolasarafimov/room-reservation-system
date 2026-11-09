package mk.finki.roomreservation;

import mk.finki.roomreservation.config.SecurityConfig;
import mk.finki.roomreservation.controller.ApprovalController;
import mk.finki.roomreservation.controller.UserController;
import mk.finki.roomreservation.model.ApprovalRequest;
import mk.finki.roomreservation.model.ApprovalResult;
import mk.finki.roomreservation.model.UserOption;
import mk.finki.roomreservation.service.ApprovalService;
import mk.finki.roomreservation.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        UserController.class,
        ApprovalController.class
})
@Import(SecurityConfig.class)
class SecurityAuthorizationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private ApprovalService approvalService;

    @Test
    void anonymousUserCannotAccessUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "REGULAR")
    void regularUserCannotAccessUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "REGULAR")
    void regularUserCannotAccessPendingReservations() throws Exception {
        mockMvc.perform(get("/api/reservations/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "APPROVER")
    void approverCanAccessPendingReservations() throws Exception {
        mockMvc.perform(get("/api/reservations/pending"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "APPROVER")
    void approvalWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/api/approvals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reservationId": 7,
                                  "approverId": 3,
                                  "decision": "approved",
                                  "note": "Security test"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "REGULAR")
    void regularUserCannotApproveWithValidCsrf() throws Exception {
        mockMvc.perform(post("/api/approvals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reservationId": 7,
                                  "approverId": 2,
                                  "decision": "approved",
                                  "note": "Security test"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "approver1", roles = "APPROVER")
    void approvalUsesAuthenticatedUserId() throws Exception {
        when(userService.findByUsername("approver1"))
                .thenReturn(new UserOption(
                        3,
                        "approver1",
                        "approver1@example.test",
                        "Demo Reservation Approver",
                        "approver"
                ));

        when(approvalService.approveOrReject(any(ApprovalRequest.class)))
                .thenReturn(new ApprovalResult(
                        1,
                        7,
                        "approved",
                        "approved",
                        "2026-11-09T00:00:00"
                ));

        mockMvc.perform(post("/api/approvals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reservationId": 7,
                                  "approverId": 2,
                                  "decision": "approved",
                                  "note": "Security test"
                                }
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<ApprovalRequest> captor =
                ArgumentCaptor.forClass(ApprovalRequest.class);

        verify(approvalService).approveOrReject(captor.capture());

        assertThat(captor.getValue().approverId()).isEqualTo(3);
    }
}