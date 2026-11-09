package mk.finki.roomreservation.controller;

import mk.finki.roomreservation.model.CreateReservationRequest;
import mk.finki.roomreservation.model.ReservationDetails;
import mk.finki.roomreservation.model.UserOption;
import mk.finki.roomreservation.service.ReservationService;
import mk.finki.roomreservation.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReservationController {

    private final ReservationService reservationService;
    private final UserService userService;

    public ReservationController(ReservationService reservationService, UserService userService) {
        this.reservationService = reservationService;
        this.userService = userService;
    }

    @PostMapping("/api/reservations")
    public ReservationDetails createReservation(@RequestBody CreateReservationRequest request,
                                                 Authentication authentication) {
        UserOption user = userService.findByUsername(authentication.getName());
        CreateReservationRequest trustedRequest = new CreateReservationRequest(
                user.userId(), request.reservationDate(), request.startTime(),
                request.endTime(), request.roomId(), request.equipment());
        return reservationService.createReservation(trustedRequest);
    }

    @GetMapping("/api/reservations/{id}")
    public ReservationDetails findReservation(@PathVariable int id, Authentication authentication) {
        UserOption user = userService.findByUsername(authentication.getName());
        boolean elevated = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_APPROVER"));
        if (!elevated && !userService.ownsReservation(id, user.userId())) {
            throw new AccessDeniedException("Reservation is not accessible to this user");
        }
        return reservationService.findReservationDetails(id);
    }
}
