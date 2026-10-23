-- =============================================================
-- CLINIC APPOINTMENT BOOKING SYSTEM - EMERGENCY RESPONSE MODULE
-- DATABASE DDL SCHEMA SCRIPT (MySQL 8.0)
-- For MySQL Workbench / College Project Viva Demonstration
-- =============================================================

USE clinic_db;

-- 1. Update Users Table Role Enum to include HOSPITAL
ALTER TABLE users MODIFY COLUMN role ENUM('PATIENT', 'DOCTOR', 'ADMIN', 'HOSPITAL') NOT NULL;

-- 2. Hospitals Table
CREATE TABLE IF NOT EXISTS hospitals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    address VARCHAR(500),
    area VARCHAR(100),
    phone VARCHAR(20),
    latitude DOUBLE,
    longitude DOUBLE,
    service_radius_km DOUBLE DEFAULT 15.0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    has_emergency_services BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. Ambulances Table
CREATE TABLE IF NOT EXISTS ambulances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hospital_id BIGINT NOT NULL,
    vehicle_number VARCHAR(30) NOT NULL UNIQUE,
    driver_name VARCHAR(100),
    driver_phone VARCHAR(20),
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    current_latitude DOUBLE,
    current_longitude DOUBLE,
    location_updated_at DATETIME,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE
);

-- 4. Emergency Requests Table
CREATE TABLE IF NOT EXISTS emergency_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    patient_id BIGINT,
    category VARCHAR(50),
    description TEXT NOT NULL,
    callback_phone VARCHAR(20) NOT NULL,
    latitude DOUBLE,
    longitude DOUBLE,
    location_accuracy DOUBLE,
    manual_address VARCHAR(500),
    landmark VARCHAR(200),
    area VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    INDEX idx_emergency_status (status),
    INDEX idx_emergency_user (user_id)
);

-- 5. Hospital Responses Table (For Tracking Accept/Reject per Hospital)
CREATE TABLE IF NOT EXISTS hospital_responses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    emergency_request_id BIGINT NOT NULL,
    hospital_id BIGINT NOT NULL,
    response_type VARCHAR(20) NOT NULL,
    reason VARCHAR(255),
    responded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (emergency_request_id) REFERENCES emergency_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE,
    UNIQUE KEY uk_emergency_hospital (emergency_request_id, hospital_id)
);

-- 6. Emergency Assignments Table (Links Emergency to Hospital, Doctor & Ambulance)
CREATE TABLE IF NOT EXISTS emergency_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    emergency_request_id BIGINT NOT NULL UNIQUE,
    hospital_id BIGINT NOT NULL,
    doctor_id BIGINT,
    ambulance_id BIGINT,
    accepted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    doctor_assigned_at DATETIME,
    ambulance_dispatched_at DATETIME,
    doctor_acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    doctor_acknowledged_at DATETIME,
    estimated_arrival_minutes INT DEFAULT 15,
    FOREIGN KEY (emergency_request_id) REFERENCES emergency_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id),
    FOREIGN KEY (doctor_id) REFERENCES doctors(id),
    FOREIGN KEY (ambulance_id) REFERENCES ambulances(id)
);

-- 7. Ambulance Location Pings (GPS Tracking Updates)
CREATE TABLE IF NOT EXISTS ambulance_locations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    emergency_request_id BIGINT NOT NULL,
    ambulance_id BIGINT NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    speed_kmph DOUBLE,
    is_simulated BOOLEAN NOT NULL DEFAULT FALSE,
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (emergency_request_id) REFERENCES emergency_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (ambulance_id) REFERENCES ambulances(id) ON DELETE CASCADE,
    INDEX idx_amb_emergency_recorded (emergency_request_id, recorded_at)
);

-- 8. Emergency Status History Table (Audit Trail)
CREATE TABLE IF NOT EXISTS emergency_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    emergency_request_id BIGINT NOT NULL,
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    changed_by_user_id BIGINT,
    notes VARCHAR(500),
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (emergency_request_id) REFERENCES emergency_requests(id) ON DELETE CASCADE,
    INDEX idx_hist_emergency_changed (emergency_request_id, changed_at)
);
