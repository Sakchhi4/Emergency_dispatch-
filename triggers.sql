-- ============================================================
-- Audit Log Triggers for Emergency Dispatch System
-- Run this AFTER schema.sql to enable automatic change tracking
-- ============================================================
USE emergency_dispatch;

-- ── INCIDENTS ──

DELIMITER //

CREATE TRIGGER trg_incidents_insert AFTER INSERT ON incidents
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, record_id, action, changed_by, new_values)
    VALUES ('incidents', NEW.incident_id, 'INSERT', 'System',
        JSON_OBJECT('caller', NEW.caller_name, 'type', NEW.incident_type,
                     'severity', NEW.severity, 'status', NEW.status,
                     'address', NEW.incident_address));
END //

CREATE TRIGGER trg_incidents_update AFTER UPDATE ON incidents
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, record_id, action, changed_by, old_values, new_values)
    VALUES ('incidents', NEW.incident_id, 'UPDATE', 'System',
        JSON_OBJECT('status', OLD.status, 'severity', OLD.severity),
        JSON_OBJECT('status', NEW.status, 'severity', NEW.severity));
END //

CREATE TRIGGER trg_incidents_delete AFTER DELETE ON incidents
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, record_id, action, changed_by, old_values)
    VALUES ('incidents', OLD.incident_id, 'DELETE', 'System',
        JSON_OBJECT('caller', OLD.caller_name, 'type', OLD.incident_type,
                     'status', OLD.status, 'address', OLD.incident_address));
END //

-- ── AMBULANCES ──

CREATE TRIGGER trg_ambulances_insert AFTER INSERT ON ambulances
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, record_id, action, changed_by, new_values)
    VALUES ('ambulances', NEW.ambulance_id, 'INSERT', 'System',
        JSON_OBJECT('vehicle_no', NEW.vehicle_no, 'type', NEW.vehicle_type, 'status', NEW.status));
END //

CREATE TRIGGER trg_ambulances_update AFTER UPDATE ON ambulances
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, record_id, action, changed_by, old_values, new_values)
    VALUES ('ambulances', NEW.ambulance_id, 'UPDATE', 'System',
        JSON_OBJECT('status', OLD.status),
        JSON_OBJECT('status', NEW.status));
END //

CREATE TRIGGER trg_ambulances_delete AFTER DELETE ON ambulances
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, record_id, action, changed_by, old_values)
    VALUES ('ambulances', OLD.ambulance_id, 'DELETE', 'System',
        JSON_OBJECT('vehicle_no', OLD.vehicle_no, 'type', OLD.vehicle_type));
END //

-- ── RESPONDERS ──

CREATE TRIGGER trg_responders_insert AFTER INSERT ON responders
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, record_id, action, changed_by, new_values)
    VALUES ('responders', NEW.responder_id, 'INSERT', 'System',
        JSON_OBJECT('name', NEW.full_name, 'role', NEW.role, 'status', NEW.status));
END //

CREATE TRIGGER trg_responders_update AFTER UPDATE ON responders
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, record_id, action, changed_by, old_values, new_values)
    VALUES ('responders', NEW.responder_id, 'UPDATE', 'System',
        JSON_OBJECT('status', OLD.status),
        JSON_OBJECT('status', NEW.status));
END //

CREATE TRIGGER trg_responders_delete AFTER DELETE ON responders
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, record_id, action, changed_by, old_values)
    VALUES ('responders', OLD.responder_id, 'DELETE', 'System',
        JSON_OBJECT('name', OLD.full_name, 'role', OLD.role));
END //

-- ── DISPATCH LOG ──

CREATE TRIGGER trg_dispatch_insert AFTER INSERT ON dispatch_log
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, record_id, action, changed_by, new_values)
    VALUES ('dispatch_log', NEW.dispatch_id, 'INSERT', 'System',
        JSON_OBJECT('incident_id', NEW.incident_id, 'ambulance_id', NEW.ambulance_id,
                     'responder_id', NEW.responder_id, 'status', NEW.status));
END //

CREATE TRIGGER trg_dispatch_update AFTER UPDATE ON dispatch_log
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, record_id, action, changed_by, old_values, new_values)
    VALUES ('dispatch_log', NEW.dispatch_id, 'UPDATE', 'System',
        JSON_OBJECT('status', OLD.status),
        JSON_OBJECT('status', NEW.status));
END //

-- ── HOSPITALS ──

CREATE TRIGGER trg_hospitals_insert AFTER INSERT ON hospitals
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, record_id, action, changed_by, new_values)
    VALUES ('hospitals', NEW.hospital_id, 'INSERT', 'System',
        JSON_OBJECT('name', NEW.name, 'address', NEW.address, 'beds', NEW.total_beds));
END //

CREATE TRIGGER trg_hospitals_update AFTER UPDATE ON hospitals
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, record_id, action, changed_by, old_values, new_values)
    VALUES ('hospitals', NEW.hospital_id, 'UPDATE', 'System',
        JSON_OBJECT('available_beds', OLD.available_beds),
        JSON_OBJECT('available_beds', NEW.available_beds));
END //

DELIMITER ;

-- Done! Now any INSERT/UPDATE/DELETE on incidents, ambulances, responders,
-- dispatch_log, and hospitals will be automatically logged to audit_log.
