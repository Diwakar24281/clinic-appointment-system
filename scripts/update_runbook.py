import os
import openpyxl
from openpyxl.styles import Font, Alignment, PatternFill, Border, Side

def populate_runbook():
    src_file = "SIST_Student_FullStack_Project_Runbook.xlsx"
    if not os.path.exists(src_file):
        raise FileNotFoundError(f"{src_file} not found")

    wb = openpyxl.load_workbook(src_file)

    # Styles
    font_title = Font(name="Calibri", size=14, bold=True, color="1E3A8A")
    font_header = Font(name="Calibri", size=11, bold=True, color="FFFFFF")
    font_bold = Font(name="Calibri", size=10, bold=True)
    font_regular = Font(name="Calibri", size=10)
    
    fill_header = PatternFill(start_color="1E3A8A", end_color="1E3A8A", fill_type="solid")
    fill_alt = PatternFill(start_color="F8FAFC", end_color="F8FAFC", fill_type="solid")
    
    thin_border = Border(
        left=Side(style='thin', color='CBD5E1'),
        right=Side(style='thin', color='CBD5E1'),
        top=Side(style='thin', color='CBD5E1'),
        bottom=Side(style='thin', color='CBD5E1')
    )

    # =========================================================================
    # SHEET 1: 1. Student Info & Instructions
    # =========================================================================
    ws1 = wb["1. Student Info & Instructions"]
    ws1["A1"] = "🎓 SATHYABAMA INSTITUTE OF SCIENCE AND TECHNOLOGY (SIST)"
    ws1["A2"] = "Department of Computer Science and Engineering — Artificial Intelligence (CSE AI)"
    ws1["B2"] = "Capstone Runbook — TEAM-14"
    
    ws1["A4"] = "📌 CAPSTONE PROJECT CHARTER & TEAM SPECIFICATIONS"
    ws1["A5"] = "Student 1 (Lead Architect):"
    ws1["B5"] = "N.Diwakar"
    ws1["C5"] = "Student 1 Register No:"
    ws1["D5"] = "44731156"

    ws1["A6"] = "Student 2 (Frontend Specialist):"
    ws1["B6"] = "Alexiya Fernando"
    ws1["C6"] = "Student 2 Register No:"
    ws1["D6"] = "44731004"

    ws1["A7"] = "Department & Year:"
    ws1["B7"] = "Computer Science and Engineering - AI (3rd Year)"
    ws1["C7"] = "Batch / Section:"
    ws1["D7"] = "2024-2028 / Section A5 (Team ID: TEAM-14)"

    ws1["A8"] = "Official Project Title:"
    ws1["B8"] = "Clinic Appointment Booking System"
    ws1["C8"] = "Project Domain:"
    ws1["D8"] = "Healthcare Informatics / Full-Stack Web + Cloud Application"

    ws1["A9"] = "Primary Technology Stack:"
    ws1["B9"] = "Java 17, Spring Boot 3.2.5, Spring Data JPA, Hibernate, MySQL 8.0, HTML5, CSS3, JavaScript (ES6+ Fetch API)"
    ws1["C9"] = "Project Trainer / Mentor:"
    ws1["D9"] = "Placement Training Cell / SIST Academic Lead"

    ws1["A10"] = "Sprint Start Date:"
    ws1["B10"] = "2026-09-01 (Phase 1: Project Charter Kickoff)"
    ws1["C10"] = "Target Deployment & Viva Date:"
    ws1["D10"] = "2026-09-19 (Phase 4: Final Viva & Live Demo)"

    # =========================================================================
    # SHEET 2: 2. Phase 1 - Problem
    # =========================================================================
    ws2 = wb["2. Phase 1 - Problem"]
    ws2["A1"] = "📋 PHASE 1: DEFINE — PROBLEM STATEMENT, MOSCOW TIERS & NFRS"
    ws2["A3"] = "Dimension"
    ws2["B3"] = "Guiding Engineering Question / Explanation"
    ws2["C3"] = "Student Team Project Response (TEAM-14: Clinic Appointment Booking System)"

    p1_data = [
        ("1. Project Title", "What is the official title of your full-stack web/AI application?", "Clinic Appointment Booking System"),
        ("2. Real-World Problem", "What painful manual problem does this project eliminate? Who is impacted?", 
         "Traditional outpatient clinics suffer from chaotic walk-ins, excessive waiting times (often 2+ hours), frequent double-booking scheduling conflicts, lack of live queue visibility for waiting patients, and vulnerable paper-based prescriptions prone to illegibility, physical damage, and loss. This causes high patient dissatisfaction, doctor scheduling inefficiencies, and administrative burnout."),
        ("3. Target User Personas", "Who are the primary end-users of this application? (List specific roles)",
         "1. Outpatient Patient: Self-registers, searches specialized doctors, views real-time 30-min slot availability, books appointments with instant token generation, checks live queue status, and views/prints digital prescriptions.\n2. Consulting Doctor: Manages daily patient queue, calls next waiting patient, conducts clinical consultations, and issues structured multi-medicine electronic prescriptions.\n3. Clinic Administrator: Oversees clinic operations, configures doctor profiles & weekly availability schedules, manages medical specializations, and monitors aggregate clinic KPI metrics."),
        ("4. Proposed Technical Solution", "How does your full-stack application architecturally resolve the problem?",
         "A decoupled 3-tier full-stack architecture:\n- Tier 1 (Client Presentation): Semantic HTML5, responsive CSS3 Grid/Flexbox, and Vanilla JavaScript (ES6+) Fetch API interfacing with REST endpoints across dedicated Patient, Doctor, Admin, and Live Public Waiting Room views.\n- Tier 2 (Application & Business Engine): Java 17 Spring Boot 3.2.5 RESTful backend with robust service-layer validation enforcing 30-minute slot segmentation, strict double-booking rejection, doctor-isolated daily sequential token generation, and BCrypt security.\n- Tier 3 (Relational Persistence): MySQL 8.0 normalized database (3NF) maintaining ACID transaction compliance across users, doctors, schedules, appointments, prescriptions, and medicines."),
        ("5. MoSCoW Prioritization", "Categorize project features into the 4 MoSCoW prioritization tiers.",
         "• MUST HAVE (P0): Patient registration & BCrypt login; Doctor directory with 30-min slot picker; Concurrency-safe appointment booking (409 Conflict check); Doctor daily queue & 'Call Next Patient'; Electronic multi-row prescription generator; Live 5s waiting room display board.\n• SHOULD HAVE (P1): Admin doctor scheduling (Mon-Sat 9AM-1PM); Specialization management; Printable PDF-style prescription invoice; Appointment cancellation.\n• COULD HAVE (P2): Email/SMS notification alerts; Patient appointment history filtering; Doctor fee revenue analytics.\n• WON'T HAVE (P3 - Out of Scope): Commercial paid payment gateway integration; Telemedicine video streaming; Multi-hospital cross-tenant federation."),
        ("6. Non-Functional Requirements (NFRs)", "Specify the 4 key engineering NFRs with quantifiable metrics.",
         "1. Performance & Latency: REST API response time < 200ms for slot queries and appointment creations under 100 concurrent requests.\n2. Concurrency & Data Integrity: 100% elimination of double-booking collisions via database-level unique constraint uk_doctor_slot (doctor_id, appointment_date, appointment_time) and pessimistic transaction locking.\n3. Security & Compliance: BCrypt password hashing (strength 10), role-based route authentication (PATIENT, DOCTOR, ADMIN), and sanitized input DTO validation preventing SQL injection and XSS.\n4. Availability & Reliability: Live waiting room poller resilient against transient network drops with automatic 5-second backoff recovery; zero data loss with MySQL InnoDB transactional engine."),
        ("7. Core Value Metric (KPI)", "How will you quantify the technical success of this application?", "100% elimination of double-booking collisions, 60% reduction in patient clinic waiting time via real-time queue tracking, sub-200ms REST API response latency, and 100% digital retention of clinical prescriptions."),
        ("8. Scope Exclusions (Out of Scope)", "What complex features are intentionally deferred from this MVP?", "Third-party commercial SMS/WhatsApp paid hardware gateways, live payment gateway escrow processing, and multi-hospital SaaS federation are scheduled for post-MVP production iterations.")
    ]

    for i, (dim, q, resp) in enumerate(p1_data, start=4):
        ws2.cell(row=i, column=1, value=dim)
        ws2.cell(row=i, column=2, value=q)
        ws2.cell(row=i, column=3, value=resp)

    # =========================================================================
    # SHEET 3: 3. Phase 1 - User Stories
    # =========================================================================
    ws3 = wb["3. Phase 1 - User Stories"]
    ws3["A1"] = "📝 PHASE 1: DEFINE — USER STORIES & GIVEN/WHEN/THEN ACCEPTANCE CRITERIA"
    
    stories = [
        ("US-01", "Outpatient Patient", "Book an appointment slot for a selected doctor and date", 
         "I can secure a verified consultation time slot without manual physical queuing.",
         "Criterion 1: Given a registered patient selects a doctor and future date, When they choose an available 30-minute time slot and confirm, Then the system creates an appointment, assigns a sequential token (e.g. Token #1), and returns HTTP 201 Created.\nCriterion 2: Given two patients simultaneously select the exact same doctor, date, and 10:00 AM slot, When both submit booking requests, Then the first request succeeds (201 Created) and the second request is immediately rejected with HTTP 409 Conflict.",
         "P0 (Must Have)"),
        ("US-02", "Outpatient Patient", "Browse doctors by specialization and search by doctor name", 
         "I can quickly find the appropriate medical specialist and check consultation fees.",
         "Criterion 1: Given a patient navigates to the Doctors directory, When the page loads, Then all active doctors are rendered in responsive cards showing name, specialization, qualification, experience, fee, and weekly schedule.\nCriterion 2: Given the doctor list is displayed, When the patient types a doctor's name or filters by specialization dropdown, Then the doctor list filters instantly in real-time without page reload.",
         "P0 (Must Have)"),
        ("US-03", "Consulting Doctor", "View today's queue and call the next waiting patient", 
         "I can streamline consultation flow and eliminate waiting area confusion.",
         "Criterion 1: Given a logged-in doctor opens the Doctor Dashboard, When the page renders, Then today's patient queue is displayed in chronological order sorted by sequential token number with statuses (BOOKED, IN_CONSULTATION, COMPLETED).\nCriterion 2: Given waiting patients exist in the queue, When the doctor clicks 'Call Next Patient', Then the selected patient's status transitions to IN_CONSULTATION and updates synchronously across the dashboard and public waiting room display.",
         "P0 (Must Have)"),
        ("US-04", "Consulting Doctor", "Record clinical diagnosis, notes, and issue multi-row digital prescriptions", 
         "Patients receive clear, legible digital prescriptions and visit history is saved permanently.",
         "Criterion 1: Given a doctor is in consultation with an active patient, When the doctor enters diagnosis, clinical notes, and adds multiple medicine items (name, dosage, frequency, duration, instructions), Then clicking 'Save Prescription' persists the record and updates the appointment status to COMPLETED.\nCriterion 2: Given a completed appointment, When the patient or doctor opens the digital prescription page, Then a clean, printable PDF-style medical prescription document is rendered with all medication details.",
         "P0 (Must Have)"),
        ("US-05", "Waiting Patient / Public Display", "View live waiting room display board with real-time queue status", 
         "Patients in waiting area know exactly when their turn is approaching without crowding reception.",
         "Criterion 1: Given the public waiting room display board is open on a clinic monitor, When doctor consultation statuses change, Then the display updates 'Current in Consultation', 'Next in Line', and 'Total Waiting' within 5 seconds via background polling without page refresh.\nCriterion 2: Given no patients are currently in consultation, When the queue is fetched, Then the board gracefully displays 'No patient currently in consultation' and shows the upcoming scheduled tokens.",
         "P1 (High)"),
        ("US-06", "Clinic Administrator", "Manage doctor profiles, specializations, and weekly working schedules", 
         "The clinic can dynamically configure doctor rosters and consultation hours.",
         "Criterion 1: Given an authenticated administrator, When they create a new doctor with user credentials, specialization, fee, and weekly schedule (e.g. Mon-Sat 09:00-13:00), Then the new doctor is persisted in MySQL and becomes immediately available in patient slot booking.\nCriterion 2: Given the admin dashboard is loaded, When stats are requested, Then aggregate KPI cards display total doctors, registered patients, today's total appointments, and completed visits accurately.",
         "P1 (High)")
    ]

    for i, (sid, persona, action, benefit, criteria, prio) in enumerate(stories, start=4):
        ws3.cell(row=i, column=1, value=sid)
        ws3.cell(row=i, column=2, value=persona)
        ws3.cell(row=i, column=3, value=action)
        ws3.cell(row=i, column=4, value=benefit)
        ws3.cell(row=i, column=5, value=criteria)
        ws3.cell(row=i, column=6, value=prio)

    # =========================================================================
    # SHEET 4: 4. Phase 2 - Draw.io Arch
    # =========================================================================
    ws4 = wb["4. Phase 2 - Draw.io Arch"]
    ws4["A1"] = "📐 PHASE 2: DESIGN — SYSTEM ARCHITECTURE, PORTS & DRAW.IO SPECIFICATIONS"
    
    arch_specs = [
        ("Step 1: Access Modeling Tool", "Open https://app.diagrams.net (Free Draw.io). Create a blank diagram and select Software Architecture shapes.", "Tool: Draw.io Web App (https://app.diagrams.net - Exported as PNG/SVG in project repository presentation/ directory)"),
        ("Step 2: Tier 1 — Client Layer", "Draw a 'Browser' container representing the Client Frontend.\nSpecify: Origin Port 5500 / 8080, HTML5 UI, CSS Grid Layout, Vanilla JS Fetch API.", "Tier 1: Client Web Application (HTML5, CSS3 Grid/Flexbox, Vanilla JS ES6+, Fetch API, Port 8080/Static & Port 5500 Dev Server - Patient Portal, Doctor Console, Admin Dashboard & Public Waiting Room Display)"),
        ("Step 3: Network API Contract", "Draw a bidirectional arrow between Client and Backend Server.\nLabel: 'REST API over HTTP/1.1 (JSON Payload, CORS Headers)'.", "Protocol: HTTP/1.1 REST API over Port 8080 (JSON payloads, @CrossOrigin enabled, Standard HTTP Status Codes: 200 OK, 201 Created, 400 Bad Request, 404 Not Found, 409 Conflict)"),
        ("Step 4: Tier 2 — Backend Engine", "Draw a 'Web Server' box representing Java Spring Boot 3 / Python FastAPI.\nShow 3 internal layers: Controller (REST) -> Service (Validation) -> Repository (JPA).", "Tier 2: Spring Boot 3.2.5 REST Engine (Port 8080: Embedded Tomcat, REST Controllers -> Service Layer with 30-min Slot Validation & Concurrency Double-Booking Lock -> Spring Data JPA Repositories -> Hibernate ORM)"),
        ("Step 5: Database Connection", "Draw an arrow from Backend Service to Database.\nLabel: 'JDBC Connection Pool (TCP/IP Port 3306) / SQLAlchemy'.", "Driver: MySQL Connector/J with HikariCP Connection Pool over TCP/IP (Port 3306, serverTimezone=UTC, allowPublicKeyRetrieval=true)"),
        ("Step 6: Tier 3 — Relational DB", "Draw a 'Database Cylinder' shape representing MySQL 8.0 on disk.\nLabel: Target schema and tables (clinic_db schema).", "Tier 3: MySQL 8.0 Relational Database (Port 3306: clinic_db schema with 8 normalized 3NF tables: users, specializations, doctors, doctor_availability, patients, appointments, prescriptions, prescription_medicines with composite unique constraints)"),
        ("Step 7: Data Flow & Concurrency Pipeline", "Detail end-to-end data flow for appointment booking and queue polling.", "1. Client dispatches Fetch API POST /api/appointments JSON payload to Port 8080.\n2. AppointmentController routes to AppointmentService within @Transactional boundary.\n3. Service performs slot overlap check and queries AppointmentRepository.findMaxTokenNumberByDoctorAndDate.\n4. Hibernate issues INSERT into appointments with uk_doctor_slot constraint.\n5. MySQL commits transaction; Spring Boot returns HTTP 201 with generated Token #.\n6. Waiting Room poller on Port 8080 triggers GET /api/queue/doctor/{id}/today every 5000ms to update live queue DOM."),
        ("Step 8: Diagram Embed / Repository Link", "Export your Draw.io diagram as high-resolution PNG or shareable link and paste here:", "Draw.io Architecture Diagram saved in repository at presentation/clinic_appointment_system_presentation.pdf and presentation/presentation.html illustrating decoupled 3-tier layout across Port 5500/8080 -> Port 8080 -> Port 3306.")
    ]

    for i, (step, guide, spec) in enumerate(arch_specs, start=4):
        ws4.cell(row=i, column=1, value=step)
        ws4.cell(row=i, column=2, value=guide)
        ws4.cell(row=i, column=3, value=spec)

    # =========================================================================
    # SHEET 5: 5. Phase 2 - Database Schema
    # =========================================================================
    ws5 = wb["5. Phase 2 - Database Schema"]
    ws5["A1"] = "🗄️ PHASE 2: DESIGN — MYSQL RELATIONAL DATABASE SCHEMA (3NF DATA DICTIONARY)"
    
    # Let's ensure the complete data dictionary matching schema.sql
    schema_rows = [
        # users
        ("users", "id", "BIGINT AUTO_INCREMENT", "PRIMARY KEY", "NOT NULL", "Unique surrogate primary key identifier for system users"),
        ("users", "name", "VARCHAR(100)", "NONE", "NOT NULL", "Full legal name of the user (Patient, Doctor, or Admin)"),
        ("users", "email", "VARCHAR(100)", "UNIQUE (uk_user_email)", "NOT NULL", "Unique user email address used as login username credential"),
        ("users", "password", "VARCHAR(255)", "NONE", "NOT NULL", "BCrypt hashed secure password string (cost factor 10)"),
        ("users", "role", "ENUM('PATIENT', 'DOCTOR', 'ADMIN')", "NONE", "NOT NULL", "User authorization role discriminator enum"),
        ("users", "created_at", "TIMESTAMP", "NONE", "DEFAULT CURRENT_TIMESTAMP", "User account registration timestamp"),
        # specializations
        ("specializations", "id", "BIGINT AUTO_INCREMENT", "PRIMARY KEY", "NOT NULL", "Unique specialization primary key identifier"),
        ("specializations", "name", "VARCHAR(100)", "UNIQUE (uk_spec_name)", "NOT NULL", "Medical specialization department title (e.g. Cardiology, Pediatrics)"),
        ("specializations", "description", "VARCHAR(255)", "NONE", "NULL", "Detailed clinical scope description of the medical department"),
        # doctors
        ("doctors", "id", "BIGINT AUTO_INCREMENT", "PRIMARY KEY", "NOT NULL", "Unique doctor entity primary key identifier"),
        ("doctors", "user_id", "BIGINT", "FOREIGN KEY & UNIQUE", "NOT NULL", "1:1 reference to users(id) ON DELETE CASCADE"),
        ("doctors", "specialization_id", "BIGINT", "FOREIGN KEY", "NOT NULL", "Many:1 reference to specializations(id)"),
        ("doctors", "consultation_fee", "DECIMAL(10,2)", "NONE", "NOT NULL DEFAULT 0.00", "Outpatient doctor consultation fee amount in INR"),
        ("doctors", "is_available", "BOOLEAN", "NONE", "NOT NULL DEFAULT TRUE", "Doctor active practicing availability flag"),
        # doctor_availability
        ("doctor_availability", "id", "BIGINT AUTO_INCREMENT", "PRIMARY KEY", "NOT NULL", "Unique availability schedule primary key identifier"),
        ("doctor_availability", "doctor_id", "BIGINT", "FOREIGN KEY", "NOT NULL", "Many:1 reference to doctors(id) ON DELETE CASCADE"),
        ("doctor_availability", "day_of_week", "ENUM('MONDAY'..'SUNDAY')", "NONE", "NOT NULL", "Working day of week enum for scheduled clinical shifts"),
        ("doctor_availability", "start_time", "TIME", "NONE", "NOT NULL", "Shift consultation start time (e.g. 09:00:00)"),
        ("doctor_availability", "end_time", "TIME", "NONE", "NOT NULL", "Shift consultation end time (e.g. 13:00:00)"),
        ("doctor_availability", "uk_doc_day", "UNIQUE KEY", "COMPOSITE UNIQUE", "CONSTRAINT", "Unique constraint on (doctor_id, day_of_week) preventing overlapping shifts"),
        # patients
        ("patients", "id", "BIGINT AUTO_INCREMENT", "PRIMARY KEY", "NOT NULL", "Unique patient medical profile primary key identifier"),
        ("patients", "user_id", "BIGINT", "FOREIGN KEY & UNIQUE", "NOT NULL", "1:1 reference to users(id) ON DELETE CASCADE"),
        ("patients", "phone", "VARCHAR(20)", "NONE", "NOT NULL", "Patient primary contact telephone number"),
        ("patients", "dob", "DATE", "NONE", "NULL", "Patient date of birth for age calculation and clinical records"),
        ("patients", "gender", "VARCHAR(10)", "NONE", "NULL", "Patient gender identifier (MALE, FEMALE, OTHER)"),
        ("patients", "address", "VARCHAR(255)", "NONE", "NULL", "Patient residential postal address"),
        # appointments
        ("appointments", "id", "BIGINT AUTO_INCREMENT", "PRIMARY KEY", "NOT NULL", "Unique appointment booking record primary key identifier"),
        ("appointments", "patient_id", "BIGINT", "FOREIGN KEY", "NOT NULL", "Many:1 reference to patients(id)"),
        ("appointments", "doctor_id", "BIGINT", "FOREIGN KEY", "NOT NULL", "Many:1 reference to doctors(id)"),
        ("appointments", "appointment_date", "DATE", "NONE", "NOT NULL", "Calendar date of scheduled medical consultation"),
        ("appointments", "appointment_time", "TIME", "NONE", "NOT NULL", "Booked 30-minute consultation time slot (e.g. 10:00:00)"),
        ("appointments", "token_number", "INT", "NONE", "NOT NULL", "Sequential daily queue token number per doctor per date (1, 2, 3...)"),
        ("appointments", "status", "ENUM('BOOKED','IN_CONSULTATION','COMPLETED','CANCELLED')", "NONE", "DEFAULT 'BOOKED'", "Consultation workflow lifecycle state enum"),
        ("appointments", "created_at", "TIMESTAMP", "NONE", "DEFAULT CURRENT_TIMESTAMP", "Appointment booking creation timestamp"),
        ("appointments", "uk_doctor_slot", "UNIQUE KEY", "COMPOSITE UNIQUE", "CONSTRAINT", "Strict unique constraint on (doctor_id, appointment_date, appointment_time) preventing double-booking"),
        # prescriptions
        ("prescriptions", "id", "BIGINT AUTO_INCREMENT", "PRIMARY KEY", "NOT NULL", "Unique clinical prescription document primary key identifier"),
        ("prescriptions", "appointment_id", "BIGINT", "FOREIGN KEY & UNIQUE", "NOT NULL", "1:1 reference to appointments(id) ON DELETE CASCADE"),
        ("prescriptions", "diagnosis", "TEXT", "NONE", "NOT NULL", "Doctor diagnostic assessment and clinical observations"),
        ("prescriptions", "notes", "TEXT", "NONE", "NULL", "Clinical advice, dietary precautions, and follow-up guidance"),
        ("prescriptions", "created_at", "TIMESTAMP", "NONE", "DEFAULT CURRENT_TIMESTAMP", "Prescription generation timestamp"),
        # prescription_medicines
        ("prescription_medicines", "id", "BIGINT AUTO_INCREMENT", "PRIMARY KEY", "NOT NULL", "Unique prescribed medication item primary key identifier"),
        ("prescription_medicines", "prescription_id", "BIGINT", "FOREIGN KEY", "NOT NULL", "Many:1 reference to prescriptions(id) ON DELETE CASCADE"),
        ("prescription_medicines", "medicine_name", "VARCHAR(150)", "NONE", "NOT NULL", "Pharmaceutical brand or generic medicine name (e.g. Paracetamol, Amlodipine)"),
        ("prescription_medicines", "dosage", "VARCHAR(50)", "NONE", "NOT NULL", "Medication dosage quantity/strength (e.g. 500mg, 1 tablet, 5ml)"),
        ("prescription_medicines", "frequency", "VARCHAR(50)", "NONE", "NOT NULL", "Dosage schedule frequency (e.g. 1-0-1, 1-1-1, 0-0-1 after meals)"),
        ("prescription_medicines", "duration", "VARCHAR(50)", "NONE", "NOT NULL", "Course duration length (e.g. 5 days, 10 days, 1 month)"),
        ("prescription_medicines", "instructions", "VARCHAR(255)", "NONE", "NULL", "Specific administration instructions (e.g. Take with warm water after food)")
    ]

    for i, row_data in enumerate(schema_rows, start=4):
        for col_idx, val in enumerate(row_data, start=1):
            ws5.cell(row=i, column=col_idx, value=val)

    # =========================================================================
    # SHEET 6: 6. Phase 2 - REST API Specs
    # =========================================================================
    ws6 = wb["6. Phase 2 - REST API Specs"]
    ws6["A1"] = "🌐 PHASE 2: DESIGN — RESTFUL API SPECIFICATIONS & POSTMAN CONTRACTS"
    
    api_specs = [
        ("API-01", "POST", "/api/auth/register", '{\n  "name": "Jane Doe",\n  "email": "jane@clinic.com",\n  "password": "password123",\n  "role": "PATIENT",\n  "phone": "9876543210"\n}', "201 Created", '{\n  "id": 5,\n  "name": "Jane Doe",\n  "email": "jane@clinic.com",\n  "role": "PATIENT",\n  "message": "User registered successfully"\n}', "400 Bad Request (Email already registered / validation error)"),
        ("API-02", "POST", "/api/auth/login", '{\n  "email": "patient@clinic.com",\n  "password": "patient123"\n}', "200 OK", '{\n  "id": 1,\n  "email": "patient@clinic.com",\n  "fullName": "John Doe",\n  "role": "PATIENT",\n  "token": "authenticated"\n}', "400 Bad Request / 401 Unauthorized (Invalid email or password)"),
        ("API-03", "GET", "/api/doctors", "None (Empty Body)", "200 OK", '[\n  {\n    "id": 1,\n    "name": "Dr. Ramesh Kumar",\n    "specializationName": "Cardiology",\n    "consultationFee": 500.00,\n    "available": true\n  }\n]', "500 Internal Server Error"),
        ("API-04", "GET", "/api/doctors/{id}/slots?date=2026-09-20", "None (Query Param: date)", "200 OK", '{\n  "doctorId": 1,\n  "doctorName": "Dr. Ramesh Kumar",\n  "date": "2026-09-20",\n  "slots": [\n    {"slotTime": "09:00", "available": true},\n    {"slotTime": "09:30", "available": false}\n  ]\n}', "404 Not Found (Doctor not found or no schedule on this day)"),
        ("API-05", "POST", "/api/appointments", '{\n  "doctorId": 1,\n  "appointmentDate": "2026-09-20",\n  "slotTime": "09:00",\n  "reasonForVisit": "Chest pain consultation"\n}', "201 Created", '{\n  "id": 101,\n  "tokenNumber": 1,\n  "doctorName": "Dr. Ramesh Kumar",\n  "appointmentDate": "2026-09-20",\n  "slotTime": "09:00",\n  "status": "CONFIRMED"\n}', "400 Bad Request (Missing fields) / 409 Conflict (Slot already booked)"),
        ("API-06", "GET", "/api/appointments/patient/{patientId}", "None (Patient ID in Path)", "200 OK", '[\n  {\n    "id": 101,\n    "doctorName": "Dr. Ramesh Kumar",\n    "appointmentDate": "2026-09-20",\n    "slotTime": "09:00",\n    "tokenNumber": 1,\n    "status": "CONFIRMED"\n  }\n]', "404 Not Found (Patient not found)"),
        ("API-07", "PUT", "/api/appointments/{id}/status", '{\n  "status": "IN_CONSULTATION"\n}', "200 OK", '{\n  "id": 101,\n  "status": "IN_CONSULTATION",\n  "updatedAt": "2026-09-19T10:30:00Z"\n}', "404 Not Found / 400 Bad Request (Invalid status transition)"),
        ("API-08", "POST", "/api/prescriptions", '{\n  "appointmentId": 101,\n  "diagnosis": "Mild Hypertension",\n  "notes": "Maintain low-sodium diet and exercise",\n  "medicines": [\n    {\n      "medicineName": "Amlodipine 5mg",\n      "dosage": "1 tablet",\n      "frequency": "0-0-1",\n      "duration": "15 days",\n      "instructions": "After dinner"\n    }\n  ]\n}', "201 Created", '{\n  "id": 51,\n  "appointmentId": 101,\n  "diagnosis": "Mild Hypertension",\n  "medicines": [\n    {\n      "id": 81,\n      "medicineName": "Amlodipine 5mg",\n      "dosage": "1 tablet",\n      "frequency": "0-0-1",\n      "duration": "15 days"\n    }\n  ]\n}', "400 Bad Request / 404 Not Found (Appointment does not exist)"),
        ("API-09", "GET", "/api/queue/doctor/{doctorId}/today", "None (Doctor ID in Path)", "200 OK", '{\n  "doctorId": 1,\n  "doctorName": "Dr. Ramesh Kumar",\n  "currentPatient": {\n    "tokenNumber": 1,\n    "patientName": "John Doe",\n    "slotTime": "09:00"\n  },\n  "nextPatient": {\n    "tokenNumber": 2,\n    "patientName": "Jane Smith",\n    "slotTime": "09:30"\n  },\n  "waitingCount": 3\n}', "404 Not Found (Doctor does not exist)"),
        ("API-10", "GET", "/api/admin/stats", "None (Empty Body)", "200 OK", '{\n  "totalDoctors": 6,\n  "totalPatients": 24,\n  "todayAppointments": 12,\n  "completedAppointments": 8\n}', "500 Internal Server Error")
    ]

    for i, (aid, verb, route, req, status, succ, err) in enumerate(api_specs, start=4):
        ws6.cell(row=i, column=1, value=aid)
        ws6.cell(row=i, column=2, value=verb)
        ws6.cell(row=i, column=3, value=route)
        ws6.cell(row=i, column=4, value=req)
        ws6.cell(row=i, column=5, value=status)
        ws6.cell(row=i, column=6, value=succ)
        ws6.cell(row=i, column=7, value=err)

    # =========================================================================
    # SHEET 7: 7. Phase 3 - AI Tools Log
    # =========================================================================
    ws7 = wb["7. Phase 3 - AI Tools Log"]
    ws7["A1"] = "🤖 PHASE 3: DEVELOP — CARE AI PROMPT ENGINEERING & AUDIT LOG"
    
    ai_logs = [
        ("AI-01", "Antigravity IDE / Gemini 3.7", "Generate Spring Boot JPA Entities with Bidirectional Relations & JSON Controls",
         "Context: Healthcare clinic appointment booking system with MySQL.\nAction: Generate Java 17 JPA Entities for Doctor, DoctorAvailability, Patient, Appointment, Prescription, and PrescriptionMedicine with bidirectional mappings, CascadeType, and JsonIgnore to prevent Jackson recursion.\nResult: Clean JPA entity classes.\nExample: Include composite unique constraint on (doctor_id, appointment_date, slot_time).",
         "Generated User.java, Doctor.java, Appointment.java, Prescription.java with @Entity, @Table(uniqueConstraints=...), and @OneToMany relations.",
         "Verified entity relationships. Added @JsonIgnore on parent back-references and transitioned to DTOs to prevent Jackson circular reference infinite recursion."),
        ("AI-02", "Antigravity IDE / Gemini 3.7", "Generate Dynamic 30-Min Slot Grid & Interactive Slot Picker UI",
         "Context: Clinic patient appointment booking portal.\nAction: Create CSS Grid component for doctor 30-minute consultation slots. Style available slots in vibrant green with hover elevation, and booked/disabled slots in muted gray.\nResult: Modern responsive CSS Grid with dynamic state transitions.\nExample: Mobile viewport breakpoint at 768px.",
         "Generated .slot-grid with repeat(auto-fit, minmax(90px, 1fr)) and state badges (.slot-available, .slot-booked) with interactive click event handlers.",
         "Tested across mobile (375px) and desktop (1920px) viewports. Slot buttons dynamically re-render upon date selection change via async Fetch API."),
        ("AI-03", "Antigravity IDE / Gemini 3.7", "Strict Concurrency & Double-Booking Exception Handling",
         "Context: Multi-user appointment booking API.\nAction: Design thread-safe booking logic in AppointmentService using database-level unique constraints and Spring Data JPA transaction isolation.\nResult: Service method + @ExceptionHandler.\nExample: SlotAlreadyBookedException returning HTTP 409 Conflict.",
         "Implemented existsByDoctorIdAndAppointmentDateAndSlotTime check + custom SlotAlreadyBookedException + GlobalExceptionHandler (@RestControllerAdvice).",
         "Tested parallel curl/Postman requests targeting the same slot; 1st received 201 Created and 2nd was cleanly rejected with 409 Conflict."),
        ("AI-04", "Antigravity IDE / Gemini 3.7", "5-Second Live Queue Polling without WebSocket Overhead",
         "Context: Clinic waiting room public display board.\nAction: Write lightweight JavaScript periodic poller using Fetch API and setInterval to refresh active consultation and next token every 5000ms.\nResult: Robust JS poller with error recovery.\nExample: Graceful error handling if server restarts.",
         "Generated waiting-room.js with setInterval(fetchQueueStatus, 5000) and DOM diffing to prevent visual screen flicker.",
         "Verified in browser. Display transitions automatically within 5 seconds when doctor clicks 'Call Next Patient' in consultation console.")
    ]

    for i, (lid, tool, task, care, code, review) in enumerate(ai_logs, start=4):
        ws7.cell(row=i, column=1, value=lid)
        ws7.cell(row=i, column=2, value=tool)
        ws7.cell(row=i, column=3, value=task)
        ws7.cell(row=i, column=4, value=care)
        ws7.cell(row=i, column=5, value=code)
        ws7.cell(row=i, column=6, value=review)

    # =========================================================================
    # SHEET 8: 8. Phase 3 - Frontend UI
    # =========================================================================
    ws8 = wb["8. Phase 3 - Frontend UI"]
    ws8["A1"] = "🖥️ PHASE 3: DEVELOP — FRONTEND UI IMPLEMENTATION (HTML5, CSS3 & JS)"
    
    frontend_ui = [
        ("1. Semantic Skeleton & Layout", "<header>, <nav>, <main>, <section>, <footer>, CSS Custom Properties, Flexbox Navbar", "src/main/resources/static/pages/index.html, css/style.css", "Implemented modern medical design system with CSS tokens (--primary: #0284c7, --success: #16a34a), accessible typography, and mobile hamburger navigation.", "Completed"),
        ("2. Doctor Directory & 30-Min Slot Picker", "<form>, <input type=\"date\">, CSS Grid, dynamically rendered slot buttons", "src/main/resources/static/pages/doctors.html, pages/book-appointment.html", "Real-time interactive slot selector that dynamically queries doctor availability schedule on date change, rendering 30-minute clickable slot buttons with instant visual feedback.", "Completed"),
        ("3. Doctor Consultation & Prescription Builder", "<textarea>, dynamic multi-row <table id=\"medsTable\">, '+ Add Medicine' button", "src/main/resources/static/pages/consultation.html, js/doctor.js", "Interactive doctor consultation console enabling real-time clinical notes entry and dynamic client-side medicine row additions (Dosage, Frequency, Duration, Instructions) before submission.", "Completed"),
        ("4. Real-time Live Waiting Room Display", "High-contrast dashboard cards, CSS Pulse animations, polling indicators", "src/main/resources/static/pages/waiting-room.html, js/waiting-room.js", "Dedicated high-visibility display board optimized for clinic waiting room monitors showing 'Current in Consultation', 'Next in Line', and 'Total Waiting' counters with smooth transitions.", "Completed"),
        ("5. Admin Management & KPI Dashboard", "Tabular data grid, modal forms, SVG metric cards, 1-click status triggers", "src/main/resources/static/pages/admin-dashboard.html, pages/manage-doctors.html", "Comprehensive administrative portal featuring clinic KPI metrics, doctor weekly schedule roster configuration, medical specialization management, and master audit log.", "Completed")
    ]

    for i, (comp, tech, path, notes, status) in enumerate(frontend_ui, start=4):
        ws8.cell(row=i, column=1, value=comp)
        ws8.cell(row=i, column=2, value=tech)
        ws8.cell(row=i, column=3, value=path)
        ws8.cell(row=i, column=4, value=notes)
        ws8.cell(row=i, column=5, value=status)

    # =========================================================================
    # SHEET 9: 9. Phase 3 - Backend & DB
    # =========================================================================
    ws9 = wb["9. Phase 3 - Backend & DB"]
    ws9["A1"] = "⚙️ PHASE 3: DEVELOP — BACKEND ARCHITECTURE & MYSQL PERSISTENCE (SPRING BOOT)"
    
    backend_rows = [
        ("1. Configuration & Security", "application.properties, DataLoader.java, SecurityConfig", "@Configuration, @Bean, BCryptPasswordEncoder", "Configured MySQL 8.0 JDBC connection pool (HikariCP, port 3306), Hibernate ddl-auto=update, and automatic seeding of default admin, doctors, specializations, and schedules on startup.", "Connected & Seeded"),
        ("2. JPA Entities", "User.java, Doctor.java, Patient.java, Appointment.java, Prescription.java, PrescriptionMedicine.java", "@Entity, @Table, @Id, @GeneratedValue, @ManyToOne, @OneToMany, @OneToOne", "Mapped normalized 3NF relational schema to Java classes with cascading rules, column constraints, and JSON serialization annotations.", "Auto-generated in MySQL"),
        ("3. Data Repositories (DAO)", "AppointmentRepository.java, DoctorRepository.java, PrescriptionRepository.java, UserRepository.java", "@Repository, JpaRepository, @Query", "Implemented custom Spring Data JPA queries for date-range appointment lookups, isolated sequential token generation (findMaxTokenNumberByDoctorAndDate), and availability slots.", "Zero boilerplate SQL"),
        ("4. Business Services", "AppointmentService.java, QueueService.java, PrescriptionService.java, AuthService.java", "@Service, @Transactional, @Autowired", "Implemented core business logic: strict 30-min slot segmentation, concurrency lock on double-bookings, automatic sequential token generation, and consultation completion state transitions.", "All tests passed"),
        ("5. REST Controllers & Handlers", "AppointmentController.java, DoctorController.java, QueueController.java, PrescriptionController.java, GlobalExceptionHandler.java", "@RestController, @RequestMapping, @CrossOrigin, @PostMapping, @GetMapping, @RestControllerAdvice", "Exposed RESTful endpoints returning standardized JSON response envelopes, handling exceptions via @RestControllerAdvice with HTTP 200, 201, 400, 404, 409.", "Tested & Validated")
    ]

    for i, (layer, file_cls, annot, logic, res) in enumerate(backend_rows, start=4):
        ws9.cell(row=i, column=1, value=layer)
        ws9.cell(row=i, column=2, value=file_cls)
        ws9.cell(row=i, column=3, value=annot)
        ws9.cell(row=i, column=4, value=logic)
        ws9.cell(row=i, column=5, value=res)

    # =========================================================================
    # SHEET 10: 10. Phase 3 - Integration
    # =========================================================================
    ws10 = wb["10. Phase 3 - Integration"]
    ws10["A1"] = "🔌 PHASE 3: DEVELOP — FULL-STACK WIRING, FETCH API & CORS INTEGRATION"
    
    integration_rows = [
        ("1. User Authentication & Session Management", "localStorage + Fetch API", "Intercepts login form submit, dispatches POST /api/auth/login, persists authenticated user session role (PATIENT, DOCTOR, ADMIN) in browser localStorage, and executes client-side route guard protection.", "Session persists smoothly"),
        ("2. Real-Time Slot Availability Query", "fetch(GET /api/doctors/{id}/slots?date=...)", "On date input change, triggers async fetch to calculate available slots against doctor working hours and existing bookings; renders clickable slot buttons dynamically with instant state update.", "Zero-latency slot display"),
        ("3. Concurrency Double-Booking Interception", "fetch(POST /api/appointments) + Error parsing", "Submits appointment JSON payload; if another user claimed the slot milliseconds earlier, captures HTTP 409 Conflict and alerts patient immediately without crashing or reloading page.", "Clean conflict alert handled"),
        ("4. Dynamic Multi-Row Prescription Submission", "DOM Traversal + JSON.stringify", "Traverses dynamic HTML prescription table rows, serializes medicine array into nested JSON structure, and dispatches POST /api/prescriptions to persist complete medical record.", "Full prescription saved in 1 shot"),
        ("5. Periodic 5-Second Waiting Room Polling", "setInterval() + async fetch()", "Periodically queries GET /api/queue/doctor/{id}/today every 5 seconds, updating waiting room consultation badges and token counters dynamically without visual flicker.", "Real-time queue board live")
    ]

    for i, (ms, tech, details, outcome) in enumerate(integration_rows, start=4):
        ws10.cell(row=i, column=1, value=ms)
        ws10.cell(row=i, column=2, value=tech)
        ws10.cell(row=i, column=3, value=details)
        ws10.cell(row=i, column=4, value=outcome)

    # =========================================================================
    # SHEET 11: 11. Phase 4 - Bug Log
    # =========================================================================
    ws11 = wb["11. Phase 4 - Bug Log"]
    ws11["A1"] = "🐛 PHASE 4: DEPLOY — SYSTEMATIC BUG DIAGNOSIS & RESOLUTION LOG"
    
    bug_rows = [
        ("BUG-01", "CORS policy blocked API requests when accessing frontend on alternate local origin (e.g. port 5500 Live Server).", "Chrome DevTools Console", "Spring Boot backend on port 8080 did not include Access-Control-Allow-Origin response headers for cross-origin frontend requests.", "Added @CrossOrigin(origins = \"*\") on all @RestController classes and configured static resources in Spring Boot.", "Student Team"),
        ("BUG-02", "Race condition: simultaneous booking requests for the exact same doctor, date, and slot created duplicate appointments.", "MySQL Workbench & JMeter Concurrency Test", "Application logic alone had a microsecond gap between availability check and JPA save().", "Added database-level composite unique constraint on (doctor_id, appointment_date, appointment_time) and custom SlotAlreadyBookedException mapped to HTTP 409 Conflict.", "Student Team"),
        ("BUG-03", "Infinite JSON recursion / StackOverflowError when serializing Prescription and PrescriptionMedicine entities.", "Spring Boot Terminal Stack Trace", "Bidirectional JPA @OneToMany and @ManyToOne references caused Jackson JSON serializer to loop indefinitely between parent and child objects.", "Decoupled entities from API responses by implementing lightweight DTO records (PrescriptionResponse, PrescriptionMedicineDto).", "Student Team"),
        ("BUG-04", "Appointment date shifted backwards by 1 day due to UTC/Local timezone serialization mismatch.", "Chrome DevTools Network Tab", "Using java.util.Date caused JSON serializer to format timestamps with UTC offset that parsed into previous day locally.", "Refactored all entity and DTO date fields to ISO java.time.LocalDate and java.time.LocalTime with @JsonFormat(pattern = \"yyyy-MM-dd\").", "Student Team")
    ]

    for i, (bid, symp, tool, cause, fix, who) in enumerate(bug_rows, start=4):
        ws11.cell(row=i, column=1, value=bid)
        ws11.cell(row=i, column=2, value=symp)
        ws11.cell(row=i, column=3, value=tool)
        ws11.cell(row=i, column=4, value=cause)
        ws11.cell(row=i, column=5, value=fix)
        ws11.cell(row=i, column=6, value=who)

    # =========================================================================
    # SHEET 12: 12. Phase 4 - Cloud & Pitch
    # =========================================================================
    ws12 = wb["12. Phase 4 - Cloud & Pitch"]
    ws12["A1"] = "🚀 PHASE 4: DEPLOY — CLOUD DEPLOYMENT & 3-MINUTE VIVA PITCH SCRIPT"
    
    ws12["A4"] = "Public GitHub Repository"
    ws12["B4"] = "GitHub"
    ws12["C4"] = "https://github.com/Diwakar24281/clinic-appointment-system"
    ws12["D4"] = "Verified Active & Public"

    ws12["A5"] = "Live Frontend Web Application"
    ws12["B5"] = "Embedded Tomcat / GitHub Pages / Port 8080"
    ws12["C5"] = "http://localhost:8080/pages/index.html (Unified Spring Boot Embedded Tomcat Distribution)"
    ws12["D5"] = "Live & Responsive"

    ws12["A6"] = "Live Backend REST API"
    ws12["B6"] = "Spring Boot 3.2.5 REST Server"
    ws12["C6"] = "http://localhost:8080/api/appointments (Port 8080 RESTful API)"
    ws12["D6"] = "Responding 200 OK / 201 Created"

    ws12["A7"] = "Recruiter README.md File"
    ws12["B7"] = "GitHub Markdown"
    ws12["C7"] = "Complete README.md with Architecture Diagram, Entity Schemas, Postman API Specs, Seed Credentials & Live Demo Guide"
    ws12["D7"] = "Complete"

    # Pitch script divided across team members
    ws12["A10"] = "🎤 3-MINUTE TECHNICAL VIVA PITCH SCRIPT (DIVIDED ACROSS TEAM-14 MEMBERS)"
    
    pitch_data = [
        ("Minute 1: Problem & System Scope (Lead Architect - N.Diwakar)", 
         "Good morning respected evaluators! We are TEAM-14, presenting the Clinic Appointment Booking and Queue Management System.\n"
         "Traditional outpatient clinics face severe operational bottlenecks: chaotic walk-in lines, patient wait times exceeding 2 hours, frequent double-booking scheduling collisions, lack of live queue visibility for waiting patients, and fragile paper prescriptions prone to damage and loss.\n"
         "To solve this, our full-stack solution delivers automated 30-minute slot reservation, strict database-level double-booking prevention, automated doctor-isolated daily token generation (Token #1, #2...), a live 5-second public waiting room display board, and structured digital multi-medicine prescriptions."),
        
        ("Minute 2: 3-Tier Architecture & Tech Stack (Lead Architect - N.Diwakar & Frontend Specialist - Alexiya Fernando)", 
         "Architecturally, our system is engineered across 3 robust, decoupled tiers:\n"
         "• Tier 1 (Frontend): Developed using semantic HTML5, CSS3 Grid/Flexbox design tokens, and modular Vanilla JavaScript (ES6+) Fetch API interfacing seamlessly across Patient, Doctor, Admin, and Public Waiting Room portals.\n"
         "• Tier 2 (Backend Engine): Java 17 Spring Boot 3.2.5 REST backend with a strict 3-layer architecture: Controllers managing HTTP contracts, Service layer enforcing business validation and concurrency locks, and Spring Data JPA repositories.\n"
         "• Tier 3 (Database): MySQL 8.0 normalized in 3NF with composite unique constraints (uk_doctor_slot) and HikariCP connection pooling, ensuring strict ACID transactions and sub-200ms latency."),
        
        ("Minute 3: Live Demonstration & Robustness (Frontend Specialist - Alexiya Fernando)", 
         "During our live demonstration:\n"
         "1. Patient Portal: A patient selects Dr. Ramesh Kumar, chooses a 10:00 AM slot, and receives instant Token #1. When another user attempts to book the identical slot, the system immediately rejects it with HTTP 409 Conflict.\n"
         "2. Doctor Dashboard: The doctor clicks 'Call Next Patient', transitioning the token status to IN_CONSULTATION, which immediately reflects on the live waiting room display board via 5-second asynchronous polling.\n"
         "3. Digital Prescription: The doctor enters diagnosis, clinical notes, and multi-row medication items, clicking 'Save' to transition the visit to COMPLETED and generate a printable digital prescription invoice.\n"
         "We resolved CORS, Jackson circular serialization, and race condition challenges to build a production-grade system ready for deployment.")
    ]

    for i, (part, script) in enumerate(pitch_data, start=11):
        ws12.cell(row=i, column=1, value=part)
        ws12.cell(row=i, column=2, value=script)

    # Save to all target paths
    target_paths = [
        "SIST_Student_FullStack_Project_Runbook.xlsx",
        "docs/Student_FullStack_Project_Runbook.xlsx",
        "src/main/resources/static/SIST_Student_FullStack_Project_Runbook.xlsx"
    ]

    for p in target_paths:
        os.makedirs(os.path.dirname(p), exist_ok=True) if os.path.dirname(p) else None
        wb.save(p)
        print(f"Successfully populated and saved: {p}")

if __name__ == "__main__":
    populate_runbook()
