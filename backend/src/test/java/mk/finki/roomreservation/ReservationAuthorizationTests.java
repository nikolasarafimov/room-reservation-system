package mk.finki.roomreservation;

import mk.finki.roomreservation.config.SecurityConfig;
import mk.finki.roomreservation.controller.ReservationController;
import mk.finki.roomreservation.model.CreateReservationRequest;
import mk.finki.roomreservation.model.ReservationDetails;
import mk.finki.roomreservation.model.UserOption;
import mk.finki.roomreservation.service.ReservationService;
import mk.finki.roomreservation.service.UserService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@Import(SecurityConfig.class)
class ReservationAuthorizationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        when(userService.findByUsername("student1"))
                .thenReturn(new UserOption(
                        1,
                        "student1",
                        "student1@example.test",
                        "Demo Student",
                        "regular"
                ));
    }

    @Test
    @WithMockUser(username = "student1", roles = "REGULAR")
    void userCanReadOwnReservation() throws Exception {
        when(userService.ownsReservation(1, 1)).thenReturn(true);

        when(reservationService.findReservationDetails(1))
                .thenReturn(new ReservationDetails(
                        1,
                        "Demo Student",
                        "B1-101",
                        "2026-11-10",
                        "10:00",
                        "12:00",
                        "pending",
                        "No equipment"
                ));

        mockMvc.perform(get("/api/reservations/1"))
                .andExpect(status().isOk());

        verify(reservationService).findReservationDetails(1);
    }

    @Test
    @WithMockUser(username = "student1", roles = "REGULAR")
    void userCannotReadAnotherUsersReservation() throws Exception {
        when(userService.ownsReservation(2, 1)).thenReturn(false);

        mockMvc.perform(get("/api/reservations/2"))
                .andExpect(status().isForbidden());

        verify(reservationService, never())
                .findReservationDetails(2);
    }

    @Test
    @WithMockUser(username = "student1", roles = "REGULAR")
    void forgedUserIdIsIgnoredDuringReservationCreation() throws Exception {
        when(reservationService.createReservation(any(CreateReservationRequest.class)))
                .thenReturn(new ReservationDetails(
                        20,
                        "Demo Student",
                        "B1-204",
                        "2027-01-20",
                        "19:00",
                        "20:00",
                        "pending",
                        "No equipment"
                ));

        mockMvc.perform(post("/api/reservations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 2,
                                  "reservationDate": "2027-01-20",
                                  "startTime": "19:00",
                                  "endTime": "20:00",
                                  "roomId": 2,
                                  "equipment": []
                                }
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<CreateReservationRequest> captor =
                ArgumentCaptor.forClass(CreateReservationRequest.class);

        verify(reservationService).createReservation(captor.capture());

        assertThat(captor.getValue().userId()).isEqualTo(1);
    }
}