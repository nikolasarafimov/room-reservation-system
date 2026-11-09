package mk.finki.roomreservation.model;

public record UserOption(
        int userId,
        String username,
        String email,
        String fullName,
        String role
) {
}