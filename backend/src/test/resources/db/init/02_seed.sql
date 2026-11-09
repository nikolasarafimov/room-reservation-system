DO $$
BEGIN
    IF current_database() <> 'room_reservation' THEN
        RAISE EXCEPTION 'Safety check failed: expected database room_reservation, found %', current_database();
END IF;

    IF EXISTS (SELECT 1 FROM project.buildings)
       OR EXISTS (SELECT 1 FROM project.rooms)
       OR EXISTS (SELECT 1 FROM project.equipment)
       OR EXISTS (SELECT 1 FROM project.room_equipment)
       OR EXISTS (SELECT 1 FROM project.users)
       OR EXISTS (SELECT 1 FROM project.reservations)
       OR EXISTS (SELECT 1 FROM project.reservation_equipment)
       OR EXISTS (SELECT 1 FROM project.approvals) THEN
        RAISE EXCEPTION 'Seed cancelled: at least one project table contains data. No existing records were changed.';
END IF;
END;
$$;

INSERT INTO project.buildings (name, address) VALUES
                                                  ('Main Faculty Building', 'Demo Campus, Building A'),
                                                  ('Laboratory Building', 'Demo Campus, Building B'),
                                                  ('Administration Building', 'Demo Campus, Building C');

INSERT INTO project.rooms (building_id, room_code, capacity, type) VALUES
                                                                       (1, 'B1-101', 60, 'classroom'),
                                                                       (1, 'B1-204', 35, 'meeting_room'),
                                                                       (2, 'LAB-1', 25, 'lab'),
                                                                       (2, 'LAB-2', 20, 'lab'),
                                                                       (3, 'ADM-12', 10, 'office');

INSERT INTO project.equipment (name, stock_quantity) VALUES
                                                         ('Projector', 3),
                                                         ('Whiteboard', 0),
                                                         ('Microphone', 5),
                                                         ('Computer Workstation', 0),
                                                         ('Video Conference Camera', 2),
                                                         ('HDMI Cable', 12),
                                                         ('Extension Cable', 8);

INSERT INTO project.room_equipment (room_id, equipment_id, quantity) VALUES
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

INSERT INTO project.users (username, email, full_name, role) VALUES
                                                                 ('student1', 'student1@example.test', 'Demo Student', 'regular'),
                                                                 ('admin', 'admin@example.test', 'Demo Administrator', 'admin'),
                                                                 ('approver1', 'approver1@example.test', 'Demo Reservation Approver', 'approver'),
                                                                 ('professor1', 'professor1@example.test', 'Demo Professor', 'regular'),
                                                                 ('assistant1', 'assistant1@example.test', 'Demo Teaching Assistant', 'regular');

INSERT INTO project.reservations (
    room_id, user_id, reservation_date, start_time, end_time, status
) VALUES
      (1, 1, CURRENT_DATE + 2, TIME '10:00', TIME '12:00', 'pending'),
      (2, 4, CURRENT_DATE + 2, TIME '13:00', TIME '14:30', 'approved'),
      (3, 5, CURRENT_DATE + 3, TIME '09:00', TIME '11:00', 'approved'),
      (4, 1, CURRENT_DATE + 4, TIME '12:00', TIME '13:00', 'cancelled'),
      (NULL, 4, CURRENT_DATE + 5, TIME '15:00', TIME '16:00', 'rejected'),
      (1, 5, CURRENT_DATE + 6, TIME '08:00', TIME '10:00', 'approved'),
      (NULL, 1, CURRENT_DATE + 6, TIME '11:00', TIME '12:00', 'pending');

INSERT INTO project.reservation_equipment (
    reservation_id, equipment_id, requested_quantity
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

INSERT INTO project.approvals (
    reservation_id, approver_id, decision, decision_time, note
) VALUES
      (2, 3, 'approved', CURRENT_TIMESTAMP - INTERVAL '1 day', 'Approved for a department meeting.'),
      (3, 3, 'approved', CURRENT_TIMESTAMP - INTERVAL '1 day', 'Approved for a laboratory exercise.'),
      (5, 2, 'rejected', CURRENT_TIMESTAMP - INTERVAL '1 day', 'Requested portable projector was unavailable.'),
      (6, 2, 'approved', CURRENT_TIMESTAMP - INTERVAL '1 day', 'Approved for a morning lecture.');