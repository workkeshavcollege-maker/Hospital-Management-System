-- ============================================================================
-- HOSPITAL MANAGEMENT SYSTEM - ORACLE FreeSQL DML EXAMPLES
-- Course / Examination: College PAT 4 Practical Examination
-- ============================================================================
-- This script contains working, parameterized and standard Oracle SQL queries
-- matching the exact application schema: APP_USERS, PATIENTS, DOCTORS, APPOINTMENTS.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. INSERT STATEMENTS
-- ----------------------------------------------------------------------------

-- Insert Initial Administrator User (password_hash is BCrypt encoded 'Admin@123')
INSERT INTO APP_USERS (username, password_hash, full_name, user_role, is_active, created_at)
VALUES ('admin', '$2a$10$wK1b0O81QkUvdI8kXfF6iO0Zl9Y6mHzO9E2sL7Xw8tqB3fV2p6j1m', 'Chief Medical Officer Admin', 'ADMIN', 'Y', CURRENT_TIMESTAMP);

-- Insert Receptionist User
INSERT INTO APP_USERS (username, password_hash, full_name, user_role, is_active, created_at)
VALUES ('receptionist1', '$2a$10$wK1b0O81QkUvdI8kXfF6iO0Zl9Y6mHzO9E2sL7Xw8tqB3fV2p6j1m', 'Sarah Jenkins', 'RECEPTIONIST', 'Y', CURRENT_TIMESTAMP);

-- Insert Patient Record
INSERT INTO PATIENTS (full_name, date_of_birth, gender, phone, email, address, created_at)
VALUES ('John Doe', TO_DATE('1988-06-15', 'YYYY-MM-DD'), 'Male', '9876543210', 'john.doe@example.com', '42 MG Road, Bangalore', CURRENT_TIMESTAMP);

-- Insert Doctor Record
INSERT INTO DOCTORS (full_name, specialization, phone, email, consultation_fee, is_active)
VALUES ('Dr. Ananya Sharma', 'Cardiology', '9123456780', 'ananya.sharma@hospital.com', 850.00, 'Y');

-- Insert Scheduled Appointment
INSERT INTO APPOINTMENTS (patient_id, doctor_id, appointment_datetime, reason, status, created_at)
VALUES (1, 1, TO_TIMESTAMP('2026-10-15 10:30:00', 'YYYY-MM-DD HH24:MI:SS'), 'Routine cardiovascular checkup and ECG', 'SCHEDULED', CURRENT_TIMESTAMP);

COMMIT;


-- ----------------------------------------------------------------------------
-- 2. SELECT STATEMENTS
-- ----------------------------------------------------------------------------

-- Retrieve all active users
SELECT user_id, username, full_name, user_role, is_active, created_at 
FROM APP_USERS 
WHERE is_active = 'Y' 
ORDER BY username ASC;

-- Retrieve all patients ordered by most recently registered
SELECT patient_id, full_name, date_of_birth, gender, phone, email, address, created_at 
FROM PATIENTS 
ORDER BY patient_id DESC;

-- Search patients by ID or case-insensitive name match
SELECT patient_id, full_name, phone, email 
FROM PATIENTS 
WHERE patient_id = 1 
   OR LOWER(full_name) LIKE LOWER('%john%');

-- Retrieve all active doctors
SELECT doctor_id, full_name, specialization, phone, email, consultation_fee 
FROM DOCTORS 
WHERE is_active = 'Y' 
ORDER BY full_name ASC;


-- ----------------------------------------------------------------------------
-- 3. UPDATE STATEMENTS
-- ----------------------------------------------------------------------------

-- Update patient contact information
UPDATE PATIENTS 
SET phone = '9876500000', 
    email = 'john.updated@example.com', 
    address = '100 Indiranagar, Bangalore' 
WHERE patient_id = 1;

-- Update doctor consultation fee and specialization
UPDATE DOCTORS 
SET consultation_fee = 950.00, 
    specialization = 'Interventional Cardiology' 
WHERE doctor_id = 1;

-- Deactivate a doctor (soft deactivation to preserve appointment history)
UPDATE DOCTORS 
SET is_active = 'N' 
WHERE doctor_id = 1;

-- Reschedule an appointment date and time
UPDATE APPOINTMENTS 
SET appointment_datetime = TO_TIMESTAMP('2026-10-15 14:00:00', 'YYYY-MM-DD HH24:MI:SS') 
WHERE appointment_id = 1;

-- Update appointment status (e.g., mark as COMPLETED)
UPDATE APPOINTMENTS 
SET status = 'COMPLETED' 
WHERE appointment_id = 1;

COMMIT;


-- ----------------------------------------------------------------------------
-- 4. DELETE STATEMENTS & FOREIGN KEY INTEGRITY
-- ----------------------------------------------------------------------------

-- Attempting to delete a patient who has existing appointments:
-- In Oracle, this will fail with ORA-02292 (integrity constraint violated - child record found).
-- Safe application pattern: Check child count before deletion
SELECT COUNT(*) AS appointment_count 
FROM APPOINTMENTS 
WHERE patient_id = 1;

-- Delete patient only when child records do not exist:
DELETE FROM PATIENTS 
WHERE patient_id = 2 
  AND NOT EXISTS (SELECT 1 FROM APPOINTMENTS WHERE APPOINTMENTS.patient_id = PATIENTS.patient_id);

-- Delete or cancel an appointment:
DELETE FROM APPOINTMENTS 
WHERE appointment_id = 99;

COMMIT;


-- ----------------------------------------------------------------------------
-- 5. JOIN QUERIES (Multi-table Data Retrieval)
-- ----------------------------------------------------------------------------

-- Fetch appointment details with Patient and Doctor information
SELECT 
    a.appointment_id,
    a.appointment_datetime,
    a.status,
    a.reason,
    p.patient_id,
    p.full_name AS patient_name,
    p.phone AS patient_phone,
    d.doctor_id,
    d.full_name AS doctor_name,
    d.specialization AS doctor_specialization,
    d.consultation_fee
FROM APPOINTMENTS a
JOIN PATIENTS p ON a.patient_id = p.patient_id
JOIN DOCTORS d ON a.doctor_id = d.doctor_id
ORDER BY a.appointment_datetime ASC;

-- Upcoming appointments for a specific doctor
SELECT 
    a.appointment_id,
    a.appointment_datetime,
    a.reason,
    a.status,
    p.full_name AS patient_name,
    p.phone AS patient_phone
FROM APPOINTMENTS a
JOIN PATIENTS p ON a.patient_id = p.patient_id
WHERE a.doctor_id = 1 
  AND a.appointment_datetime >= CURRENT_TIMESTAMP
  AND a.status = 'SCHEDULED'
ORDER BY a.appointment_datetime ASC;


-- ----------------------------------------------------------------------------
-- 6. AGGREGATE QUERIES (Dashboard Metrics & Reporting)
-- ----------------------------------------------------------------------------

-- Dashboard Metrics: Total Patients
SELECT COUNT(*) AS total_patients FROM PATIENTS;

-- Dashboard Metrics: Total Active Doctors
SELECT COUNT(*) AS total_active_doctors FROM DOCTORS WHERE is_active = 'Y';

-- Dashboard Metrics: Total Appointments & Status Breakdown
SELECT 
    COUNT(*) AS total_appointments,
    COUNT(CASE WHEN status = 'SCHEDULED' THEN 1 END) AS scheduled_count,
    COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) AS completed_count,
    COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) AS cancelled_count
FROM APPOINTMENTS;

-- Doctor Workload Report (Group By & Count)
SELECT 
    d.doctor_id,
    d.full_name AS doctor_name,
    d.specialization,
    COUNT(a.appointment_id) AS total_appointments,
    SUM(CASE WHEN a.status = 'COMPLETED' THEN d.consultation_fee ELSE 0 END) AS estimated_revenue
FROM DOCTORS d
LEFT JOIN APPOINTMENTS a ON d.doctor_id = a.doctor_id
GROUP BY d.doctor_id, d.full_name, d.specialization
ORDER BY total_appointments DESC;


-- ----------------------------------------------------------------------------
-- 7. APPOINTMENT SEARCH AND DATE FILTERING
-- ----------------------------------------------------------------------------

-- Filter appointments for a given calendar day (Daily Appointment Report)
SELECT 
    a.appointment_id,
    TO_CHAR(a.appointment_datetime, 'YYYY-MM-DD HH24:MI') AS slot_time,
    p.full_name AS patient_name,
    d.full_name AS doctor_name,
    d.specialization,
    a.status,
    d.consultation_fee
FROM APPOINTMENTS a
JOIN PATIENTS p ON a.patient_id = p.patient_id
JOIN DOCTORS d ON a.doctor_id = d.doctor_id
WHERE TRUNC(a.appointment_datetime) = TO_DATE('2026-10-15', 'YYYY-MM-DD')
ORDER BY a.appointment_datetime ASC;

-- Filter by Doctor, Status, and Date Range
SELECT 
    a.appointment_id,
    a.appointment_datetime,
    p.full_name AS patient_name,
    a.status,
    a.reason
FROM APPOINTMENTS a
JOIN PATIENTS p ON a.patient_id = p.patient_id
WHERE a.doctor_id = 1
  AND a.status = 'SCHEDULED'
  AND a.appointment_datetime BETWEEN TO_TIMESTAMP('2026-10-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS')
                                AND TO_TIMESTAMP('2026-10-31 23:59:59', 'YYYY-MM-DD HH24:MI:SS')
ORDER BY a.appointment_datetime ASC;


-- ----------------------------------------------------------------------------
-- 8. CONCURRENCY CONTROL & 30-MINUTE SLOT OVERLAP DETECTION
-- ----------------------------------------------------------------------------

-- Transaction Block: Slot Overlap Detection (within +/- 30 minutes for same doctor)
-- Slot overlap definition: Existing appointment starts within 30 minutes before or after target time
-- Target: 2026-10-15 10:30:00, Doctor: 1
-- Excludes CANCELLED appointments. If rescheduling, excludes current appointment_id.

-- Step 8a: Pessimistic Row Lock on Doctor record to serialize booking attempts
SELECT doctor_id, is_active 
FROM DOCTORS 
WHERE doctor_id = 1 
FOR UPDATE;

-- Step 8b: Count conflicting appointments within 30-minute window
-- (Target time - 29 mins 59 secs to Target time + 29 mins 59 secs)
SELECT COUNT(*) AS conflicting_count
FROM APPOINTMENTS
WHERE doctor_id = 1
  AND status != 'CANCELLED'
  AND appointment_datetime > (TO_TIMESTAMP('2026-10-15 10:30:00', 'YYYY-MM-DD HH24:MI:SS') - INTERVAL '30' MINUTE)
  AND appointment_datetime < (TO_TIMESTAMP('2026-10-15 10:30:00', 'YYYY-MM-DD HH24:MI:SS') + INTERVAL '30' MINUTE);

-- Step 8c: If conflicting_count == 0, proceed with INSERT
INSERT INTO APPOINTMENTS (patient_id, doctor_id, appointment_datetime, reason, status, created_at)
VALUES (1, 1, TO_TIMESTAMP('2026-10-15 10:30:00', 'YYYY-MM-DD HH24:MI:SS'), 'Follow-up Consultation', 'SCHEDULED', CURRENT_TIMESTAMP);

COMMIT;
