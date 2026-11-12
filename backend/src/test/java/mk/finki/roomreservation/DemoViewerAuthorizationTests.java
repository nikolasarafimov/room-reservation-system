package mk.finki.roomreservation;

import mk.finki.roomreservation.config.SecurityConfig;
import mk.finki.roomreservation.controller.ReservationController;
import mk.finki.roomreservation.controller.RoomController;
import mk.finki.roomreservation.model.SearchRoomsRequest;
import mk.finki.roomreservation.service.ReservationService;
import mk.finki.roomreservation.service.RoomService;
import mk.finki.roomreservation.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        ReservationController.class,
        RoomController.class
})
@Import(SecurityConfig.class)
class DemoViewerAuthorizationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private ReservationService reservationService;

    @MockBean
    private RoomService roomService;

    @Test
    @WithMockUser(username = "demo_viewer", roles = "REGULAR")
    void demoViewerCannotCreateReservation() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "reservationDate": "2027-01-20",
                                  "startTime": "10:00",
                                  "endTime": "11:00",
                                  "roomId": 1,
                                  "equipment": []
                                }
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(reservationService);
    }

    @Test
    @WithMockUser(username = "demo_viewer", roles = "REGULAR")
    void demoViewerCanSearchAvailableRooms() throws Exception {
        when(roomService.searchAvailableRooms(any(SearchRoomsRequest.class)))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/rooms/search")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reservationDate": "2027-01-20",
                                  "startTime": "10:00",
                                  "endTime": "11:00",
                                  "minimumCapacity": 1,
                                  "roomType": "",
                                  "requiredEquipmentName": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(roomService)
                .searchAvailableRooms(any(SearchRoomsRequest.class));
    }
}