BEGIN;

DO $$
DECLARE
    v_blocked boolean := false;
BEGIN
    IF current_database() <> 'room_reservation' THEN
        RAISE EXCEPTION 'Wrong database: %', current_database();
    END IF;

    BEGIN
        INSERT INTO project.reservations (
            room_id, user_id, reservation_date, start_time, end_time, status
        )
        SELECT room_id, user_id, reservation_date,
               TIME '11:00', TIME '11:30', 'pending'
        FROM project.reservations
        WHERE reservation_id = 1;
    EXCEPTION WHEN check_violation THEN
        v_blocked := true;
    END;

    IF NOT v_blocked THEN
        RAISE EXCEPTION 'FAIL: overlapping reservation was not rejected';
    END IF;

    RAISE NOTICE 'PASS: overlapping room reservation is rejected';
END;
$$;

DO $$
DECLARE
    v_blocked boolean := false;
BEGIN
    BEGIN
        PERFORM 1
        FROM project.fn_approve_or_reject_reservation(
            1, 1, 'approved'::project.approval_decision_domain,
            'Temporary unauthorized-operation test'
        );
    EXCEPTION WHEN check_violation THEN
        v_blocked := true;
    END;

    IF NOT v_blocked THEN
        RAISE EXCEPTION 'FAIL: unauthorized approver was accepted';
    END IF;

    RAISE NOTICE 'PASS: regular user cannot approve a reservation';
END;
$$;

SELECT out_reservation_id, out_status, out_decision
FROM project.fn_approve_or_reject_reservation(
    1, 3, 'approved'::project.approval_decision_domain,
    'Temporary approval test - will be rolled back'
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM project.reservations
        WHERE reservation_id = 1 AND status = 'approved'
    ) THEN
        RAISE EXCEPTION 'FAIL: approved status was not saved';
    END IF;

    RAISE NOTICE 'PASS: authorized approver can approve a pending reservation';
END;
$$;

ROLLBACK;

SELECT reservation_id, status
FROM project.reservations
WHERE reservation_id = 1;

SELECT COUNT(*) AS approvals_after_rollback
FROM project.approvals;

SELECT COUNT(*) AS utilization_report_rows
FROM project.v_quarterly_room_utilization;
