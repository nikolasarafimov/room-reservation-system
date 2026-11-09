DO $$
BEGIN
    IF current_database() <> 'room_reservation' THEN
        RAISE EXCEPTION 'Expected local database room_reservation, found %', current_database();
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_namespace WHERE nspname = 'project'
    ) THEN
        RAISE EXCEPTION 'Schema project does not exist. Run 01_schema.sql first.';
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_type t
        JOIN pg_namespace n ON n.oid = t.typnamespace
        WHERE n.nspname = 'project'
          AND t.typname = 'approval_decision_domain'
    ) THEN
        CREATE DOMAIN project.approval_decision_domain AS text
            CHECK (VALUE IN ('approved', 'rejected'));
    END IF;
END;
$$;

CREATE OR REPLACE VIEW project.v_pending_reservation_details AS
SELECT
    res.reservation_id,
    u.full_name AS requester_name,
    res.reservation_date,
    res.start_time,
    res.end_time,
    r.room_code,
    b.name AS building_name,
    res.status,
    eq.requested_equipment
FROM project.reservations res
JOIN project.users u ON u.user_id = res.user_id
LEFT JOIN project.rooms r ON r.room_id = res.room_id
LEFT JOIN project.buildings b ON b.building_id = r.building_id
LEFT JOIN (
    SELECT
        re.reservation_id,
        string_agg(e.name || ' x' || re.requested_quantity, ', ' ORDER BY e.name)
            AS requested_equipment
    FROM project.reservation_equipment re
    JOIN project.equipment e ON e.equipment_id = re.equipment_id
    GROUP BY re.reservation_id
) eq ON eq.reservation_id = res.reservation_id
WHERE res.status = 'pending';

CREATE OR REPLACE VIEW project.v_quarterly_room_utilization AS
WITH summary AS (
    SELECT
        date_trunc('quarter', res.reservation_date)::date AS quarter_start,
        b.name AS building_name,
        r.room_code,
        r.type,
        r.capacity,
        COUNT(*) AS total_room_reservations,
        COUNT(*) FILTER (WHERE res.status = 'approved') AS approved_reservations,
        COUNT(*) FILTER (WHERE res.status = 'rejected') AS rejected_reservations,
        COUNT(*) FILTER (WHERE res.status = 'cancelled') AS cancelled_reservations,
        COUNT(*) FILTER (WHERE res.status = 'pending') AS pending_reservations,
        ROUND(SUM(EXTRACT(EPOCH FROM (res.end_time - res.start_time)) / 3600)::numeric, 2)
            AS requested_hours,
        ROUND(COALESCE(SUM(
            EXTRACT(EPOCH FROM (res.end_time - res.start_time)) / 3600
        ) FILTER (WHERE res.status = 'approved'), 0)::numeric, 2)
            AS approved_hours
    FROM project.reservations res
    JOIN project.rooms r ON r.room_id = res.room_id
    JOIN project.buildings b ON b.building_id = r.building_id
    GROUP BY
        date_trunc('quarter', res.reservation_date)::date,
        b.name, r.room_code, r.type, r.capacity
)
SELECT
    summary.*,
    COALESCE(ROUND(
        approved_hours / NULLIF(requested_hours, 0) * 100, 2
    ), 0)::numeric AS approved_hours_share_percent,
    DENSE_RANK() OVER (
        PARTITION BY quarter_start
        ORDER BY approved_hours DESC, total_room_reservations DESC,
                 building_name, room_code
    )::integer AS utilization_rank,
    CASE
        WHEN requested_hours = 0 THEN 'no_activity'
        WHEN approved_hours >= requested_hours * 0.75 THEN 'high_utilization'
        WHEN approved_hours >= requested_hours * 0.40 THEN 'medium_utilization'
        ELSE 'low_utilization'
    END::text AS utilization_level
FROM summary;

CREATE OR REPLACE FUNCTION project.fn_check_room_overlap()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.room_id IS NULL OR NEW.status NOT IN ('pending', 'approved') THEN
        RETURN NEW;
    END IF;

    PERFORM pg_advisory_xact_lock(
        NEW.room_id,
        NEW.reservation_date - DATE '2000-01-01'
    );

    IF EXISTS (
        SELECT 1
        FROM project.reservations other_res
        WHERE other_res.room_id = NEW.room_id
          AND other_res.reservation_date = NEW.reservation_date
          AND other_res.reservation_id <> NEW.reservation_id
          AND other_res.status IN ('pending', 'approved')
          AND NEW.start_time < other_res.end_time
          AND NEW.end_time > other_res.start_time
    ) THEN
        RAISE EXCEPTION 'Room is already reserved during the requested time interval.'
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_check_room_overlap ON project.reservations;
CREATE TRIGGER trg_check_room_overlap
BEFORE INSERT OR UPDATE OF room_id, reservation_date, start_time, end_time, status
ON project.reservations
FOR EACH ROW EXECUTE FUNCTION project.fn_check_room_overlap();

CREATE OR REPLACE FUNCTION project.fn_validate_approval()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    v_role text;
    v_status text;
BEGIN
    SELECT u.role INTO v_role
    FROM project.users u
    WHERE u.user_id = NEW.approver_id;

    IF v_role IS NULL OR v_role NOT IN ('admin', 'approver') THEN
        RAISE EXCEPTION 'User is not authorized to approve reservations.'
            USING ERRCODE = '23514';
    END IF;

    SELECT r.status INTO v_status
    FROM project.reservations r
    WHERE r.reservation_id = NEW.reservation_id
    FOR UPDATE;

    IF v_status IS DISTINCT FROM 'pending' THEN
        RAISE EXCEPTION 'Only pending reservations can be approved or rejected.'
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_validate_approval ON project.approvals;
CREATE TRIGGER trg_validate_approval
BEFORE INSERT ON project.approvals
FOR EACH ROW EXECUTE FUNCTION project.fn_validate_approval();

CREATE OR REPLACE FUNCTION project.fn_approve_or_reject_reservation(
    p_reservation_id integer,
    p_approver_id integer,
    p_decision project.approval_decision_domain,
    p_note text
)
RETURNS TABLE (
    out_approval_id integer,
    out_reservation_id integer,
    out_status text,
    out_decision text,
    out_decision_time text
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_status text;
    v_approval_id integer;
    v_decision_time timestamp;
BEGIN
    SELECT r.status INTO v_status
    FROM project.reservations r
    WHERE r.reservation_id = p_reservation_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Reservation % does not exist.', p_reservation_id;
    END IF;

    IF v_status <> 'pending' THEN
        RAISE EXCEPTION 'Reservation % is not pending.', p_reservation_id;
    END IF;

    INSERT INTO project.approvals (
        reservation_id, approver_id, decision, decision_time, note
    ) VALUES (
        p_reservation_id, p_approver_id, p_decision::text,
        CURRENT_TIMESTAMP, p_note
    )
    RETURNING approval_id, decision_time
    INTO v_approval_id, v_decision_time;

    UPDATE project.reservations
    SET status = p_decision::text
    WHERE reservation_id = p_reservation_id;

    RETURN QUERY SELECT
        v_approval_id,
        p_reservation_id,
        p_decision::text,
        p_decision::text,
        v_decision_time::text;
END;
$$;

CREATE INDEX IF NOT EXISTS idx_reservations_active_room_interval
ON project.reservations (room_id, reservation_date, start_time, end_time)
WHERE status IN ('pending', 'approved');

CREATE INDEX IF NOT EXISTS idx_reservation_equipment_equipment
ON project.reservation_equipment (equipment_id, reservation_id);
