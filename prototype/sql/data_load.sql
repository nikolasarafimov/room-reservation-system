BEGIN;

DO $$
BEGIN
    IF current_database() <> 'room_reservation_prototype' THEN
        RAISE EXCEPTION 'Safety check failed: expected database room_reservation_prototype, found %', current_database();
END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_namespace WHERE nspname = 'project'
    ) THEN
        RAISE EXCEPTION 'Schema project does not exist. Run schema_creation.sql first.';
END IF;
END;
$$;

SET search_path TO project;

TRUNCATE TABLE
    approvals,
    reservation_equipment,
    reservations,
    room_equipment,
    equipment,
    rooms,
    users,
    buildings
RESTART IDENTITY CASCADE;

INSERT INTO buildings (building_id, name, address) VALUES
                                                       (1, 'Main Faculty Building', 'Demo Campus, Building A'),
                                                       (2, 'Laboratory Building', 'Demo Campus, Building B'),
                                                       (3, 'Administration Building', 'Demo Campus, Building C');

INSERT INTO rooms (room_id, building_id, room_code, capacity, type) VALUES
                                                                        (1, 1, 'B1-101', 60, 'classroom'),
                                                                        (2, 1, 'B1-204', 35, 'meeting_room'),
                                                                        (3, 2, 'LAB-1', 25, 'lab'),
                                                                        (4, 2, 'LAB-2', 20, 'lab'),
                                                                        (5, 3, 'ADM-12', 10, 'office');

INSERT INTO equipment (equipment_id, name, stock_quantity) VALUES
                                                               (1, 'Projector', 3),
                                                               (2, 'Whiteboard', 0),
                                                               (3, 'Microphone', 5),
                                                               (4, 'Computer Workstation', 0),
                                                               (5, 'Video Conference Camera', 2),
                                                               (6, 'HDMI Cable', 12),
                                                               (7, 'Extension Cable', 8);

INSERT INTO room_equipment (room_id, equipment_id, quantity) VALUES
                                                                 (1, 1, 1),
                                                                 (1, 2, 1),
                                                                 (2, 2, 1),
                                                                 (2, 3, 2),
                                                                 (2, 5, 1),
                                                                 (3, 1, 1),
                                                                 (3, 2, 1),
                                                                 (3, 4, 15),
                                                                 (4, 2, 1),
                                                                 (4, 4, 12),
                                                                 (5, 2, 1);

INSERT INTO users (user_id, username, email, full_name, role) VALUES
                                                                  (1, 'student1', 'student1@example.test', 'Demo Student', 'regular'),
                                                                  (2, 'admin', 'admin@example.test', 'Demo Administrator', 'admin'),
                                                                  (3, 'approver1', 'approver1@example.test', 'Demo Reservation Approver', 'approver'),
                                                                  (4, 'professor1', 'professor1@example.test', 'Demo Professor', 'regular'),
                                                                  (5, 'assistant1', 'assistant1@example.test', 'Demo Teaching Assistant', 'regular');

INSERT INTO reservations (
    reservation_id,
    room_id,
    user_id,
    reservation_date,
    start_time,
    end_time,
    status
) VALUES
      (1, 1, 1, CURRENT_DATE + 2, TIME '10:00', TIME '12:00', 'pending'),
      (2, 2, 4, CURRENT_DATE + 2, TIME '13:00', TIME '14:30', 'approved'),
      (3, 3, 5, CURRENT_DATE + 3, TIME '09:00', TIME '11:00', 'approved'),
      (4, 4, 1, CURRENT_DATE + 4, TIME '12:00', TIME '13:00', 'cancelled'),
      (5, NULL, 4, CURRENT_DATE + 5, TIME '15:00', TIME '16:00', 'rejected'),
      (6, 1, 5, CURRENT_DATE + 6, TIME '08:00', TIME '10:00', 'approved'),
      (7, NULL, 1, CURRENT_DATE + 6, TIME '11:00', TIME '12:00', 'pending');

INSERT INTO reservation_equipment (
    reservation_id,
    equipment_id,
    requested_quantity
) VALUES
      (1, 6, 2),
      (1, 7, 1),
      (2, 5, 1),
      (3, 3, 1),
      (5, 1, 1),
      (5, 6, 2),
      (6, 3, 2),
      (7, 6, 3),
      (7, 7, 2);

INSERT INTO approvals (
    approval_id,
    reservation_id,
    approver_id,
    decision,
    decision_time,
    note
) VALUES
      (1, 2, 3, 'approved', CURRENT_TIMESTAMP - INTERVAL '1 day', 'Approved for a department meeting.'),
      (2, 3, 3, 'approved', CURRENT_TIMESTAMP - INTERVAL '1 day', 'Approved for a laboratory exercise.'),
      (3, 5, 2, 'rejected', CURRENT_TIMESTAMP - INTERVAL '1 day', 'Requested portable projector was unavailable.'),
      (4, 6, 2, 'approved', CURRENT_TIMESTAMP - INTERVAL '1 day', 'Approved for a morning lecture.');

SELECT setval(
               pg_get_serial_sequence('project.buildings', 'building_id'),
               (SELECT MAX(building_id) FROM project.buildings)
       );

SELECT setval(
               pg_get_serial_sequence('project.rooms', 'room_id'),
               (SELECT MAX(room_id) FROM project.rooms)
       );

SELECT setval(
               pg_get_serial_sequence('project.equipment', 'equipment_id'),
               (SELECT MAX(equipment_id) FROM project.equipment)
       );

SELECT setval(
               pg_get_serial_sequence('project.users', 'user_id'),
               (SELECT MAX(user_id) FROM project.users)
       );

SELECT setval(
               pg_get_serial_sequence('project.reservations', 'reservation_id'),
               (SELECT MAX(reservation_id) FROM project.reservations)
       );

SELECT setval(
               pg_get_serial_sequence('project.approvals', 'approval_id'),
               (SELECT MAX(approval_id) FROM project.approvals)
       );

COMMIT;