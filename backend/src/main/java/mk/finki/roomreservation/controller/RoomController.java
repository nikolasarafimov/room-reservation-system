package mk.finki.roomreservation.controller;

import mk.finki.roomreservation.model.RoomOption;
import mk.finki.roomreservation.model.SearchRoomsRequest;
import mk.finki.roomreservation.service.RoomService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/api/rooms")
    public List<RoomOption> findAllRooms() {
        return roomService.findAllRooms();
    }

    @PostMapping("/api/rooms/search")
    public List<RoomOption> searchAvailableRooms(@RequestBody SearchRoomsRequest request) {
        return roomService.searchAvailableRooms(request);
    }
}