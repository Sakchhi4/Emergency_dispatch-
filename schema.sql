-- ============================================================
-- Emergency Healthcare Response & Dispatch Tracking System
-- MySQL Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS emergency_dispatch;
USE emergency_dispatch;

-- ============================================================
-- 1. ZONES / SERVICE AREAS
-- ============================================================
CREATE TABLE zones (
    zone_id       INT AUTO_INCREMENT PRIMARY KEY,
    zone_name     VARCHAR(100) NOT NULL,
    region        VARCHAR(100),
    latitude      DECIMAL(9,6),
    longitude     DECIMAL(9,6),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 2. HOSPITALS
-- ============================================================
CREATE TABLE hospitals (
    hospital_id        INT AUTO_INCREMENT PRIMARY KEY,
    name               VARCHAR(150) NOT NULL,
    address            VARCHAR(255),
    phone              VARCHAR(20),
    zone_id            INT,
    total_beds         INT DEFAULT 0,
    available_beds     INT DEFAULT 0,
    trauma_center      BOOLEAN DEFAULT FALSE,
    latitude           DECIMAL(9,6),
    longitude          DECIMAL(9,6),
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (zone_id) REFERENCES zones(zone_id)
);

-- ============================================================
-- 3. RESPONDERS (Paramedics / EMTs / Doctors)
-- ============================================================
CREATE TABLE responders (
    responder_id   INT AUTO_INCREMENT PRIMARY KEY,
    full_name      VARCHAR(150) NOT NULL,
    role           ENUM('EMT','Paramedic','Doctor','Nurse','Dispatcher') NOT NULL,
    phone          VARCHAR(20),
    email          VARCHAR(100),
    license_no     VARCHAR(50),
    zone_id        INT,
    status         ENUM('Available','On Duty','Off Duty','On Leave') DEFAULT 'Available',
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (zone_id) REFERENCES zones(zone_id)
);

-- ============================================================
-- 4. AMBULANCES / VEHICLES
-- ============================================================
CREATE TABLE ambulances (
    ambulance_id      INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_no        VARCHAR(30) NOT NULL UNIQUE,
    vehicle_type      ENUM('BLS','ALS','Critical Care','Air Ambulance','Bicycle') DEFAULT 'BLS',
    zone_id           INT,
    status            ENUM('Available','Dispatched','En Route','At Scene','Returning','Maintenance') DEFAULT 'Available',
    last_latitude     DECIMAL(9,6),
    last_longitude    DECIMAL(9,6),
    last_updated      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (zone_id) REFERENCES zones(zone_id)
);

-- ============================================================
-- 5. PATIENTS
-- ============================================================
CREATE TABLE patients (
    patient_id     INT AUTO_INCREMENT PRIMARY KEY,
    full_name      VARCHAR(150),
    dob            DATE,
    gender         ENUM('Male','Female','Other','Unknown') DEFAULT 'Unknown',
    blood_group    VARCHAR(5),
    phone          VARCHAR(20),
    address        TEXT,
    allergies      TEXT,
    medical_history TEXT,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 6. INCIDENTS (Emergency Calls)
-- ============================================================
CREATE TABLE incidents (
    incident_id        INT AUTO_INCREMENT PRIMARY KEY,
    call_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    caller_name        VARCHAR(150),
    caller_phone       VARCHAR(20),
    incident_type      ENUM(
                         'Cardiac Arrest','Trauma','Road Accident','Fire Injury',
                         'Stroke','Respiratory','Poisoning','Maternity',
                         'Psychiatric','Unknown','Other'
                       ) NOT NULL DEFAULT 'Unknown',
    severity           ENUM('Critical','High','Medium','Low') DEFAULT 'Medium',
    description        TEXT,
    incident_address   TEXT NOT NULL,
    latitude           DECIMAL(9,6),
    longitude          DECIMAL(9,6),
    zone_id            INT,
    patient_id         INT,
    status             ENUM('Pending','Dispatched','In Progress','Resolved','Cancelled') DEFAULT 'Pending',
    resolved_at        TIMESTAMP NULL,
    notes              TEXT,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (zone_id) REFERENCES zones(zone_id),
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
);

-- ============================================================
-- 7. DISPATCH LOG
-- ============================================================
CREATE TABLE dispatch_log (
    dispatch_id        INT AUTO_INCREMENT PRIMARY KEY,
    incident_id        INT NOT NULL,
    ambulance_id       INT NOT NULL,
    responder_id       INT,
    dispatched_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    en_route_at        TIMESTAMP NULL,
    arrived_scene_at   TIMESTAMP NULL,
    departed_scene_at  TIMESTAMP NULL,
    arrived_hospital_at TIMESTAMP NULL,
    hospital_id        INT,
    response_time_min  DECIMAL(6,2) GENERATED ALWAYS AS (
                         TIMESTAMPDIFF(SECOND, dispatched_at, arrived_scene_at) / 60.0
                       ) STORED,
    status             ENUM('Dispatched','En Route','At Scene','Transporting','Completed','Cancelled') DEFAULT 'Dispatched',
    dispatcher_notes   TEXT,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (incident_id)  REFERENCES incidents(incident_id),
    FOREIGN KEY (ambulance_id) REFERENCES ambulances(ambulance_id),
    FOREIGN KEY (responder_id) REFERENCES responders(responder_id),
    FOREIGN KEY (hospital_id)  REFERENCES hospitals(hospital_id)
);

-- ============================================================
-- 8. TRIAGE RECORDS
-- ============================================================
CREATE TABLE triage_records (
    triage_id          INT AUTO_INCREMENT PRIMARY KEY,
    dispatch_id        INT NOT NULL,
    patient_id         INT,
    triage_level       ENUM('Immediate','Delayed','Minimal','Expectant') NOT NULL,
    chief_complaint    TEXT,
    vitals_bp          VARCHAR(20),
    vitals_pulse       INT,
    vitals_spo2        DECIMAL(5,2),
    vitals_temp        DECIMAL(4,1),
    gcs_score          INT COMMENT 'Glasgow Coma Scale 3-15',
    treatment_notes    TEXT,
    recorded_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (dispatch_id) REFERENCES dispatch_log(dispatch_id),
    FOREIGN KEY (patient_id)  REFERENCES patients(patient_id)
);

-- ============================================================
-- 9. EQUIPMENT INVENTORY
-- ============================================================
CREATE TABLE equipment (
    equipment_id   INT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(150) NOT NULL,
    category       VARCHAR(100),
    quantity       INT DEFAULT 0,
    ambulance_id   INT,
    last_checked   DATE,
    expiry_date    DATE,
    status         ENUM('Available','In Use','Expired','Under Maintenance') DEFAULT 'Available',
    FOREIGN KEY (ambulance_id) REFERENCES ambulances(ambulance_id)
);

-- ============================================================
-- 10. AUDIT / SYSTEM LOG
-- ============================================================
CREATE TABLE audit_log (
    log_id         INT AUTO_INCREMENT PRIMARY KEY,
    table_name     VARCHAR(100),
    record_id      INT,
    action         ENUM('INSERT','UPDATE','DELETE') NOT NULL,
    changed_by     VARCHAR(100),
    changed_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    old_values     JSON,
    new_values     JSON
);

-- ============================================================
-- INDEXES FOR PERFORMANCE
-- ============================================================
CREATE INDEX idx_incidents_status     ON incidents(status);
CREATE INDEX idx_incidents_severity   ON incidents(severity);
CREATE INDEX idx_incidents_call_time  ON incidents(call_time);
CREATE INDEX idx_dispatch_incident    ON dispatch_log(incident_id);
CREATE INDEX idx_dispatch_ambulance   ON dispatch_log(ambulance_id);
CREATE INDEX idx_ambulances_status    ON ambulances(status);
CREATE INDEX idx_responders_status    ON responders(status);

-- ============================================================
-- SAMPLE SEED DATA
-- ============================================================
INSERT INTO zones (zone_name, region) VALUES
  ('Zone Alpha', 'North City'),
  ('Zone Beta',  'South City'),
  ('Zone Gamma', 'East District');

INSERT INTO hospitals (name, address, phone, zone_id, total_beds, available_beds, trauma_center) VALUES
  ('City General Hospital',   '12 Main St',    '9000001111', 1, 300, 45, TRUE),
  ('St. Mary Medical Center', '88 Park Ave',   '9000002222', 2, 200, 30, FALSE),
  ('Eastern Trauma Center',   '5 East Blvd',   '9000003333', 3, 150, 20, TRUE);

INSERT INTO responders (full_name, role, phone, zone_id, status) VALUES
  ('Dr. Ayesha Khan',   'Doctor',     '9100001111', 1, 'Available'),
  ('Ravi Sharma',       'Paramedic',  '9100002222', 1, 'Available'),
  ('Priya Nair',        'EMT',        '9100003333', 2, 'Available'),
  ('Dispatch Officer A','Dispatcher', '9100004444', 1, 'On Duty');

INSERT INTO ambulances (vehicle_no, vehicle_type, zone_id, status) VALUES
  ('MH-AMB-001', 'ALS',          1, 'Available'),
  ('MH-AMB-002', 'BLS',          2, 'Available'),
  ('MH-AMB-003', 'Critical Care',3, 'Available');
