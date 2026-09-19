# Clinic Appointment Booking System

A full-stack, beginner-friendly **Clinic Appointment Booking System** developed with **Spring Boot 3, Spring Data JPA, MySQL 8.0, and Vanilla HTML5/CSS3/JavaScript (Fetch API)**.

---

## 🚀 Key Features

* **Patient Portal**:
  * Self-registration and secure login (BCrypt hashed passwords).
  * Search doctors by name and filter by specialization.
  * Interactive 30-minute slot picker (real-time slot availability check).
  * Strict double-booking prevention at both backend and database levels.
  * Doctor/date-isolated sequential token generation (`Token #1, #2, #3...`).
  * Appointment history with cancellation support.
  * View and print digital prescriptions.
* **Doctor Portal**:
  * Real-time dashboard showing today's queue and waiting patients.
  * "Call Next Patient" action to update queue status (`IN_CONSULTATION`).
  * Conduct consultations, enter clinical diagnosis, notes, and multi-row medication prescriptions.
* **Admin Portal**:
  * Overview KPIs (Total doctors, patients, today's appointments, completed visits).
  * Add doctors with weekly availability schedules (e.g., Mon–Sat 9AM–1PM).
  * Manage medical specializations.
  * Master audit log of all clinic appointments.
* **Live Waiting Room Display**:
  * Public display board periodically polling updated queue status via JavaScript Fetch API every 5 seconds (no WebSockets needed).
  * Displays **Current in Consultation**, **Next in Line**, and **Waiting Count**.

---

## 🛠️ Technology Stack

| Layer | Technology |
|---|---|
| **Frontend** | HTML5, CSS3, Vanilla JavaScript (ES6+), Fetch API |
| **Backend** | Java 17, Spring Boot 3.2.5, Spring Web, Spring Data JPA, Hibernate |
| **Security** | Spring Security Crypto (BCrypt password encoder) |
| **Database** | MySQL 8.0 Relational Database |
| **Build Tool** | Apache Maven 3.9+ |

---

## 📋 Prerequisites

Before running the application, ensure you have:
1. **Java Development Kit (JDK 17 or higher)** installed (`java -version`).
2. **Apache Maven 3.8+** installed (`mvn -version`).
3. **MySQL Server 8.0+** running on `localhost:3306`.

---

## ⚙️ Step-by-Step Setup & Execution

### Step 1: Configure MySQL Database
Open `src/main/resources/application.properties` and verify your MySQL root password:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/clinic_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```
> **Note**: `createDatabaseIfNotExist=true` will automatically create the `clinic_db` database for you upon startup. Hibernate will automatically create all tables.

---

### Step 2: Build the Application
In your terminal, navigate to the project directory and run:
```bash
mvn clean package -DskipTests
```

---

### Step 3: Run the Spring Boot Server
You can run the application directly using Maven:
```bash
mvn spring-boot:run
```
Or run the packaged JAR file:
```bash
java -jar target/clinic-appointment-system-1.0.0.jar
```

---

### Step 4: Access the Web Application
Open your browser and navigate to:
```
http://localhost:8080/pages/index.html
```

---

## 🔑 Preloaded Demo Credentials

On initial startup, `DataLoader.java` automatically seeds sample data:

| Role | Email | Password | Description |
|---|---|---|---|
| **Admin** | `admin@clinic.com` | `admin123` | Clinic administrator with full management access |
| **Doctor** | `dr.ramesh@clinic.com` | `doctor123` | Cardiologist with Mon-Sat 9AM-1PM schedule |
| **Doctor** | `dr.priya@clinic.com` | `doctor123` | Pediatrician with Mon-Sat 10AM-2PM schedule |
| **Doctor** | `dr.amit@clinic.com` | `doctor123` | General Physician with Mon-Sat 9AM-1PM schedule |
| **Patient** | `patient@clinic.com` | `patient123` | Demo patient account (or register a new one) |

---

## 🧪 Testing User Flows (Viva Demonstration)

### Flow 1: Patient Appointment Booking & Double-Booking Check
1. Log in as a Patient (`patient@clinic.com` / `patient123`).
2. Go to **Doctors** $\rightarrow$ Click **Book Appointment** for *Dr. Ramesh Kumar*.
3. Choose today's date or a future date $\rightarrow$ Pick an available 30-minute slot (e.g., `10:00 AM`).
4. Click **Confirm & Book Slot** $\rightarrow$ Receive **Token #1**.
5. *Double-Booking Verification*: Open another tab/incognito window, register a second patient, select the exact same doctor, date, and 10:00 AM slot. The slot will show as disabled, and attempting to book it will return a `409 Conflict` error.

### Flow 2: Doctor Queue & Consultation
1. Log in as Doctor (`dr.ramesh@clinic.com` / `doctor123`).
2. In the **Doctor Dashboard**, observe the waiting queue.
3. Click **📢 Call Next Patient**.
4. The patient's token transitions to `IN_CONSULTATION`.
5. Enter Clinical Diagnosis (e.g., *Mild Hypertension*), Clinical Advice, and add medications (e.g., *Amlodipine 5mg - 1 tablet - 0-0-1 - 15 days*).
6. Click **Save Prescription & Complete Consultation**.
7. The appointment status updates to `COMPLETED` and the printable digital prescription is generated.

### Flow 3: Live Waiting Room Display
1. Open `http://localhost:8080/pages/waiting-room.html` in a separate browser window.
2. Select *Dr. Ramesh Kumar*.
3. Observe real-time updates as tokens transition from `Next in Line` to `Current in Consultation`.
