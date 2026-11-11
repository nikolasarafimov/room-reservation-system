package mk.finki.roomreservation;

import java.io.Console;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class App {

    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Room Reservation System - Prototype");
        System.out.println("-----------------------------------");


        String dbUrl = readRequiredValue(scanner, "Database URL", "DB_URL");
        String dbUser = readRequiredValue(scanner, "Database username", "DB_USER");
        String dbPassword = readPassword(scanner);

        try {
            DatabasePool.initialize(dbUrl, dbUser, dbPassword);

            try (Connection connection = DatabasePool.getConnection()) {
                System.out.println();
                System.out.println("Connected to database successfully.");
            }

            new App().run();

        } catch (SQLException e) {
            System.out.println("Database connection failed.");
            System.out.println(e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("Application initialization failed.");
            System.out.println(e.getMessage());
        } finally {
            DatabasePool.close();
        }
    }

    private void run() {
        while (true) {
            System.out.println();
            System.out.println("Main menu");
            System.out.println("1. Search available rooms");
            System.out.println("2. Create reservation request");
            System.out.println("3. Approve or reject reservation");
            System.out.println("4. Exit");

            int choice = readInt("Choose option: ", 1, 4);

            try {
                switch (choice) {
                    case 1 -> searchAvailableRoomsScenario();
                    case 2 -> createReservationScenario();
                    case 3 -> approveOrRejectReservationScenario();
                    case 4 -> {
                        System.out.println("Application closed.");
                        return;
                    }
                    default -> System.out.println("Invalid option.");
                }
            } catch (SQLException e) {
                System.out.println("Database operation failed.");
                System.out.println(e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("Operation failed.");
                System.out.println(e.getMessage());
            }
        }
    }

    private void searchAvailableRoomsScenario() throws SQLException {
        System.out.println();
        System.out.println("UC0001 - Search available rooms");

        SearchCriteria criteria = readSearchCriteria();
        List<Room> rooms = findAvailableRooms(criteria);

        if (rooms.isEmpty()) {
            System.out.println("No available rooms were found for the selected criteria.");
            return;
        }

        System.out.println();
        System.out.println("Available rooms:");
        printRooms(rooms);
    }

    private void createReservationScenario() throws SQLException {
        System.out.println();
        System.out.println("UC0002 - Create reservation request");

        User requester = chooseUser("Choose requester:");
        LocalDate date = readDate("Reservation date (YYYY-MM-DD): ");
        LocalTime startTime = readTime("Start time (HH:MM): ");
        LocalTime endTime = readTime("End time (HH:MM): ");

        if (!endTime.isAfter(startTime)) {
            System.out.println("End time must be greater than start time.");
            return;
        }

        Integer selectedRoomId = null;

        if (readYesNo("Include room in reservation? (y/n): ")) {
            int minCapacity = readInt("Minimum capacity: ", 1, 10000);
            String roomType = readOptional("Room type filter, for example classroom/meeting_room/lab/office (empty for any): ");
            String equipmentFilter = readOptional("Required equipment in room (empty for none): ");

            SearchCriteria criteria = new SearchCriteria(date, startTime, endTime, minCapacity, roomType, equipmentFilter);
            List<Room> availableRooms = findAvailableRooms(criteria);

            if (availableRooms.isEmpty()) {
                System.out.println("No available rooms match the selected criteria.");
                return;
            }

            System.out.println();
            System.out.println("Available rooms:");
            printRooms(availableRooms);

            int roomChoice = readInt("Choose room number: ", 1, availableRooms.size());
            selectedRoomId = availableRooms.get(roomChoice - 1).roomId();
        }

        List<EquipmentRequest> equipmentRequests = new ArrayList<>();

        if (readYesNo("Request additional/general equipment? (y/n): ")) {
            equipmentRequests = chooseEquipmentRequests();
        }

        if (selectedRoomId == null && equipmentRequests.isEmpty()) {
            System.out.println("A reservation must include at least one requested resource: a room, equipment, or both.");
            return;
        }

        int reservationId;

        try (Connection connection = DatabasePool.getConnection()) {
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            try {
                reservationId = insertReservation(connection, selectedRoomId, requester.userId(), date, startTime, endTime);

                for (EquipmentRequest request : equipmentRequests) {
                    insertReservationEquipment(connection, reservationId, request.equipmentId(), request.quantity());
                }

                connection.commit();

                System.out.println();
                System.out.println("Reservation transaction committed successfully.");
                System.out.println("Reservation request created successfully.");
                System.out.println("Reservation ID: " + reservationId);

            } catch (SQLException e) {
                connection.rollback();
                System.out.println("Reservation transaction rolled back.");
                throw e;

            } finally {
                connection.setAutoCommit(true);
            }
        }

        showReservationDetails(reservationId);
    }

    private void approveOrRejectReservationScenario() throws SQLException {
        System.out.println();
        System.out.println("UC0003 - Approve or reject reservation");

        List<PendingReservation> pendingReservations = findPendingReservations();

        if (pendingReservations.isEmpty()) {
            System.out.println("There are no pending reservations.");
            return;
        }

        System.out.println();
        System.out.println("Pending reservations:");
        for (int i = 0; i < pendingReservations.size(); i++) {
            PendingReservation r = pendingReservations.get(i);
            System.out.printf(
                    "%d. Reservation %d | requester: %s | room: %s | date: %s | time: %s-%s | equipment: %s%n",
                    i + 1,
                    r.reservationId(),
                    r.requesterName(),
                    r.roomCode(),
                    r.date(),
                    r.startTime(),
                    r.endTime(),
                    r.equipmentText()
            );
        }

        int reservationChoice = readInt("Choose reservation number: ", 1, pendingReservations.size());
        PendingReservation selectedReservation = pendingReservations.get(reservationChoice - 1);

        User approver = chooseApprover();

        System.out.println("1. Approve");
        System.out.println("2. Reject");
        int decisionChoice = readInt("Choose decision: ", 1, 2);

        String decision = decisionChoice == 1 ? "approved" : "rejected";
        String note = readOptional("Decision note (optional): ");

        try (Connection connection = DatabasePool.getConnection()) {
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            try {
                updateReservationStatus(connection, selectedReservation.reservationId(), decision);
                insertApproval(connection, selectedReservation.reservationId(), approver.userId(), decision, note);

                connection.commit();

                System.out.println();
                System.out.println("Approval transaction committed successfully.");
                System.out.println("Reservation decision saved successfully.");

            } catch (SQLException e) {
                connection.rollback();
                System.out.println("Approval transaction rolled back.");
                throw e;

            } finally {
                connection.setAutoCommit(true);
            }
        }

        showReservationDetails(selectedReservation.reservationId());
    }

    private SearchCriteria readSearchCriteria() {
        LocalDate date = readDate("Reservation date (YYYY-MM-DD): ");
        LocalTime startTime = readTime("Start time (HH:MM): ");
        LocalTime endTime = readTime("End time (HH:MM): ");

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be greater than start time.");
        }

        int minCapacity = readInt("Minimum capacity: ", 1, 10000);
        String roomType = readOptional("Room type filter, for example classroom/meeting_room/lab/office (empty for any): ");
        String equipmentName = readOptional("Required equipment name (empty for none): ");

        return new SearchCriteria(date, startTime, endTime, minCapacity, roomType, equipmentName);
    }

    private List<Room> findAvailableRooms(SearchCriteria criteria) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    r.room_id,
                    r.room_code,
                    r.capacity,
                    r.type,
                    b.name AS building_name,
                    b.address
                FROM project.rooms r
                JOIN project.buildings b
                    ON b.building_id = r.building_id
                WHERE r.capacity >= ?
                """);

        List<Object> parameters = new ArrayList<>();
        parameters.add(criteria.minCapacity());

        if (!criteria.roomType().isBlank()) {
            sql.append(" AND LOWER(r.type) = LOWER(?) ");
            parameters.add(criteria.roomType());
        }

        if (!criteria.equipmentName().isBlank()) {
            sql.append("""
                    AND EXISTS (
                        SELECT 1
                        FROM project.room_equipment re
                        JOIN project.equipment e
                            ON e.equipment_id = re.equipment_id
                        WHERE re.room_id = r.room_id
                          AND LOWER(e.name) LIKE LOWER(?)
                          AND re.quantity > 0
                    )
                    """);
            parameters.add("%" + criteria.equipmentName() + "%");
        }

        sql.append("""
                AND NOT EXISTS (
                    SELECT 1
                    FROM project.reservations res
                    WHERE res.room_id = r.room_id
                      AND res.reservation_date = ?
                      AND res.status IN ('pending', 'approved')
                      AND CAST(? AS time) < res.end_time
                      AND CAST(? AS time) > res.start_time
                )
                ORDER BY r.capacity, r.room_code
                """);

        parameters.add(Date.valueOf(criteria.date()));
        parameters.add(Time.valueOf(criteria.startTime()));
        parameters.add(Time.valueOf(criteria.endTime()));

        try (Connection connection = DatabasePool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            setParameters(statement, parameters);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<Room> rooms = new ArrayList<>();

                while (resultSet.next()) {
                    rooms.add(new Room(
                            resultSet.getInt("room_id"),
                            resultSet.getString("room_code"),
                            resultSet.getInt("capacity"),
                            resultSet.getString("type"),
                            resultSet.getString("building_name"),
                            resultSet.getString("address")
                    ));
                }

                return rooms;
            }
        }
    }

    private User chooseUser(String title) throws SQLException {
        List<User> users = findUsers(false);

        if (users.isEmpty()) {
            throw new IllegalStateException("No users exist in the database.");
        }

        System.out.println();
        System.out.println(title);

        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            System.out.printf(
                    "%d. %s | username: %s | role: %s | email: %s%n",
                    i + 1,
                    user.fullName(),
                    user.username(),
                    user.role(),
                    user.email()
            );
        }

        int choice = readInt("Choose user number: ", 1, users.size());
        return users.get(choice - 1);
    }

    private User chooseApprover() throws SQLException {
        List<User> approvers = findUsers(true);

        if (approvers.isEmpty()) {
            throw new IllegalStateException("No approver or admin users exist in the database.");
        }

        System.out.println();
        System.out.println("Choose approver:");

        for (int i = 0; i < approvers.size(); i++) {
            User user = approvers.get(i);
            System.out.printf(
                    "%d. %s | username: %s | role: %s%n",
                    i + 1,
                    user.fullName(),
                    user.username(),
                    user.role()
            );
        }

        int choice = readInt("Choose approver number: ", 1, approvers.size());
        return approvers.get(choice - 1);
    }

    private List<User> findUsers(boolean approversOnly) throws SQLException {
        String sql = """
                SELECT user_id, username, email, full_name, role
                FROM project.users
                """ + (approversOnly ? "WHERE role IN ('admin', 'approver') " : "") + """
                ORDER BY full_name, username
                """;

        try (Connection connection = DatabasePool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            List<User> users = new ArrayList<>();

            while (resultSet.next()) {
                users.add(new User(
                        resultSet.getInt("user_id"),
                        resultSet.getString("username"),
                        resultSet.getString("email"),
                        resultSet.getString("full_name"),
                        resultSet.getString("role")
                ));
            }

            return users;
        }
    }

    private List<EquipmentRequest> chooseEquipmentRequests() throws SQLException {
        List<Equipment> equipmentList = findEquipment();

        if (equipmentList.isEmpty()) {
            System.out.println("No equipment exists in the database.");
            return List.of();
        }

        Map<Integer, EquipmentRequest> selected = new LinkedHashMap<>();

        while (true) {
            System.out.println();
            System.out.println("Available equipment:");

            for (int i = 0; i < equipmentList.size(); i++) {
                Equipment equipment = equipmentList.get(i);
                System.out.printf(
                        "%d. %s | stock quantity: %d%n",
                        i + 1,
                        equipment.name(),
                        equipment.stockQuantity()
                );
            }

            System.out.println("0. Finish equipment selection");

            int choice = readInt("Choose equipment number: ", 0, equipmentList.size());

            if (choice == 0) {
                break;
            }

            Equipment selectedEquipment = equipmentList.get(choice - 1);

            if (selectedEquipment.stockQuantity() <= 0) {
                System.out.println("This equipment currently has no available stock.");
                continue;
            }

            int quantity = readInt("Requested quantity: ", 1, selectedEquipment.stockQuantity());

            selected.put(
                    selectedEquipment.equipmentId(),
                    new EquipmentRequest(selectedEquipment.equipmentId(), selectedEquipment.name(), quantity)
            );

            System.out.println("Equipment added to reservation request.");
        }

        return new ArrayList<>(selected.values());
    }

    private List<Equipment> findEquipment() throws SQLException {
        String sql = """
                SELECT equipment_id, name, stock_quantity
                FROM project.equipment
                ORDER BY name
                """;

        try (Connection connection = DatabasePool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            List<Equipment> equipmentList = new ArrayList<>();

            while (resultSet.next()) {
                equipmentList.add(new Equipment(
                        resultSet.getInt("equipment_id"),
                        resultSet.getString("name"),
                        resultSet.getInt("stock_quantity")
                ));
            }

            return equipmentList;
        }
    }

    private int insertReservation(
            Connection connection,
            Integer roomId,
            int userId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) throws SQLException {

        String sql = """
                INSERT INTO project.reservations (
                    room_id,
                    user_id,
                    reservation_date,
                    start_time,
                    end_time,
                    status
                )
                VALUES (?, ?, ?, ?, ?, 'pending')
                RETURNING reservation_id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (roomId == null) {
                statement.setNull(1, Types.INTEGER);
            } else {
                statement.setInt(1, roomId);
            }

            statement.setInt(2, userId);
            statement.setDate(3, Date.valueOf(date));
            statement.setTime(4, Time.valueOf(startTime));
            statement.setTime(5, Time.valueOf(endTime));

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("reservation_id");
                }
            }
        }

        throw new SQLException("Reservation was not created.");
    }

    private void insertReservationEquipment(
            Connection connection,
            int reservationId,
            int equipmentId,
            int requestedQuantity
    ) throws SQLException {

        String sql = """
                INSERT INTO project.reservation_equipment (
                    reservation_id,
                    equipment_id,
                    requested_quantity
                )
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, reservationId);
            statement.setInt(2, equipmentId);
            statement.setInt(3, requestedQuantity);
            statement.executeUpdate();
        }
    }

    private List<PendingReservation> findPendingReservations() throws SQLException {
        String sql = """
                SELECT
                    res.reservation_id,
                    u.full_name AS requester_name,
                    COALESCE(r.room_code, 'No room') AS room_code,
                    res.reservation_date,
                    res.start_time,
                    res.end_time,
                    COALESCE(
                        STRING_AGG(e.name || ' x' || req.requested_quantity, ', ' ORDER BY e.name),
                        'No equipment'
                    ) AS equipment_text
                FROM project.reservations res
                JOIN project.users u
                    ON u.user_id = res.user_id
                LEFT JOIN project.rooms r
                    ON r.room_id = res.room_id
                LEFT JOIN project.reservation_equipment req
                    ON req.reservation_id = res.reservation_id
                LEFT JOIN project.equipment e
                    ON e.equipment_id = req.equipment_id
                WHERE res.status = 'pending'
                GROUP BY
                    res.reservation_id,
                    u.full_name,
                    r.room_code,
                    res.reservation_date,
                    res.start_time,
                    res.end_time
                ORDER BY res.reservation_date, res.start_time, res.reservation_id
                """;

        try (Connection connection = DatabasePool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            List<PendingReservation> reservations = new ArrayList<>();

            while (resultSet.next()) {
                reservations.add(new PendingReservation(
                        resultSet.getInt("reservation_id"),
                        resultSet.getString("requester_name"),
                        resultSet.getString("room_code"),
                        resultSet.getDate("reservation_date").toLocalDate(),
                        resultSet.getTime("start_time").toLocalTime(),
                        resultSet.getTime("end_time").toLocalTime(),
                        resultSet.getString("equipment_text")
                ));
            }

            return reservations;
        }
    }

    private void updateReservationStatus(
            Connection connection,
            int reservationId,
            String status
    ) throws SQLException {

        String sql = """
                UPDATE project.reservations
                SET status = ?
                WHERE reservation_id = ?
                  AND status = 'pending'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, reservationId);

            int updatedRows = statement.executeUpdate();

            if (updatedRows != 1) {
                throw new SQLException("Reservation status was not updated. It may not be pending anymore.");
            }
        }
    }

    private void insertApproval(
            Connection connection,
            int reservationId,
            int approverId,
            String decision,
            String note
    ) throws SQLException {

        String sql = """
                INSERT INTO project.approvals (
                    reservation_id,
                    approver_id,
                    decision,
                    decision_time,
                    note
                )
                VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, reservationId);
            statement.setInt(2, approverId);
            statement.setString(3, decision);

            if (note == null || note.isBlank()) {
                statement.setNull(4, Types.VARCHAR);
            } else {
                statement.setString(4, note);
            }

            statement.executeUpdate();
        }
    }

    private void showReservationDetails(int reservationId) throws SQLException {
        String sql = """
                SELECT
                    res.reservation_id,
                    res.reservation_date,
                    res.start_time,
                    res.end_time,
                    res.status,
                    COALESCE(r.room_code, 'No room') AS room_code,
                    u.full_name AS requester_name,
                    COALESCE(
                        STRING_AGG(e.name || ' x' || req.requested_quantity, ', ' ORDER BY e.name),
                        'No equipment'
                    ) AS equipment_text,
                    a.decision,
                    a.decision_time,
                    approver.full_name AS approver_name
                FROM project.reservations res
                JOIN project.users u
                    ON u.user_id = res.user_id
                LEFT JOIN project.rooms r
                    ON r.room_id = res.room_id
                LEFT JOIN project.reservation_equipment req
                    ON req.reservation_id = res.reservation_id
                LEFT JOIN project.equipment e
                    ON e.equipment_id = req.equipment_id
                LEFT JOIN project.approvals a
                    ON a.reservation_id = res.reservation_id
                LEFT JOIN project.users approver
                    ON approver.user_id = a.approver_id
                WHERE res.reservation_id = ?
                GROUP BY
                    res.reservation_id,
                    res.reservation_date,
                    res.start_time,
                    res.end_time,
                    res.status,
                    r.room_code,
                    u.full_name,
                    a.decision,
                    a.decision_time,
                    approver.full_name
                """;

        try (Connection connection = DatabasePool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, reservationId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    System.out.println();
                    System.out.println("Reservation details");
                    System.out.println("Reservation ID: " + resultSet.getInt("reservation_id"));
                    System.out.println("Requester: " + resultSet.getString("requester_name"));
                    System.out.println("Room: " + resultSet.getString("room_code"));
                    System.out.println("Date: " + resultSet.getDate("reservation_date"));
                    System.out.println("Time: " + resultSet.getTime("start_time") + " - " + resultSet.getTime("end_time"));
                    System.out.println("Equipment: " + resultSet.getString("equipment_text"));
                    System.out.println("Status: " + resultSet.getString("status"));

                    String decision = resultSet.getString("decision");

                    if (decision != null) {
                        System.out.println("Decision: " + decision);
                        System.out.println("Decision time: " + resultSet.getTimestamp("decision_time"));
                        System.out.println("Approver: " + resultSet.getString("approver_name"));
                    }
                }
            }
        }
    }

    private void printRooms(List<Room> rooms) {
        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);

            System.out.printf(
                    "%d. %s | type: %s | capacity: %d | building: %s | address: %s%n",
                    i + 1,
                    room.roomCode(),
                    room.type(),
                    room.capacity(),
                    room.buildingName(),
                    room.address()
            );
        }
    }

    private static void setParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            Object value = parameters.get(i);

            if (value instanceof Date date) {
                statement.setDate(i + 1, date);
            } else if (value instanceof Time time) {
                statement.setTime(i + 1, time);
            } else if (value instanceof Integer integer) {
                statement.setInt(i + 1, integer);
            } else {
                statement.setObject(i + 1, value);
            }
        }
    }

    private static String readRequiredValue(Scanner scanner, String label, String envName) {
        String value = System.getenv(envName);

        if (value != null && !value.isBlank()) {
            return value;
        }

        System.out.print(label + ": ");
        String input = scanner.nextLine().trim();

        if (input.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }

        return input;
    }

    private static String readPassword(Scanner scanner) {
        String envPassword = System.getenv("DB_PASSWORD");

        if (envPassword != null && !envPassword.isBlank()) {
            return envPassword;
        }

        Console console = System.console();

        if (console != null) {
            char[] password = console.readPassword("Database password: ");
            return new String(password);
        }

        System.out.print("Database password: ");
        return scanner.nextLine().trim();
    }

    private String readOptional(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("y") || input.equals("yes")) {
                return true;
            }

            if (input.equals("n") || input.equals("no")) {
                return false;
            }

            System.out.println("Please enter y or n.");
        }
    }

    private int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);

            try {
                int value = Integer.parseInt(scanner.nextLine().trim());

                if (value >= min && value <= max) {
                    return value;
                }

                System.out.println("Enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("Enter a valid number.");
            }
        }
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);

            try {
                return LocalDate.parse(scanner.nextLine().trim());
            } catch (Exception e) {
                System.out.println("Enter date in format YYYY-MM-DD.");
            }
        }
    }

    private LocalTime readTime(String prompt) {
        while (true) {
            System.out.print(prompt);

            try {
                return LocalTime.parse(scanner.nextLine().trim());
            } catch (Exception e) {
                System.out.println("Enter time in format HH:MM.");
            }
        }
    }

    private record SearchCriteria(
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            int minCapacity,
            String roomType,
            String equipmentName
    ) {
    }

    private record Room(
            int roomId,
            String roomCode,
            int capacity,
            String type,
            String buildingName,
            String address
    ) {
    }

    private record User(
            int userId,
            String username,
            String email,
            String fullName,
            String role
    ) {
    }

    private record Equipment(
            int equipmentId,
            String name,
            int stockQuantity
    ) {
    }

    private record EquipmentRequest(
            int equipmentId,
            String name,
            int quantity
    ) {
    }

    private record PendingReservation(
            int reservationId,
            String requesterName,
            String roomCode,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            String equipmentText
    ) {
    }
}