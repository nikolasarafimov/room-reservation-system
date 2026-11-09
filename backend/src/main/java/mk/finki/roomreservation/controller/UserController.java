package mk.finki.roomreservation.controller;

import mk.finki.roomreservation.model.UserOption;
import mk.finki.roomreservation.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/users")
    public List<UserOption> findAllUsers() {
        return userService.findAllUsers();
    }

    @GetMapping("/api/approvers")
    public List<UserOption> findApprovers() {
        return userService.findApprovers();
    }
}