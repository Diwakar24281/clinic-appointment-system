package com.example.clinic.config;

import com.example.clinic.entity.*;
import com.example.clinic.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SpecializationRepository specializationRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final PatientRepository patientRepository;
    private final HospitalRepository hospitalRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public DataLoader(UserRepository userRepository,
                      SpecializationRepository specializationRepository,
                      DoctorRepository doctorRepository,
                      DoctorAvailabilityRepository availabilityRepository,
                      PatientRepository patientRepository,
                      HospitalRepository hospitalRepository,
                      AmbulanceRepository ambulanceRepository,
                      PasswordEncoder passwordEncoder,
                      org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.specializationRepository = specializationRepository;
        this.doctorRepository = doctorRepository;
        this.availabilityRepository = availabilityRepository;
        this.patientRepository = patientRepository;
        this.hospitalRepository = hospitalRepository;
        this.ambulanceRepository = ambulanceRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        // Ensure database columns exist and are compatible
        try {
            jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN role VARCHAR(30) NOT NULL");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN verification_code VARCHAR(10) NULL");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN verification_expiry DATETIME(6) NULL");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN verification_attempts INT NOT NULL DEFAULT 0");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN last_verification_sent_at DATETIME(6) NULL");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN verification_purpose VARCHAR(30) NULL");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN reset_token VARCHAR(100) NULL");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN reset_token_expiry DATETIME(6) NULL");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("ALTER TABLE doctors MODIFY COLUMN specialization_id BIGINT NULL");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("ALTER TABLE doctors ADD COLUMN approval_status VARCHAR(30) NOT NULL DEFAULT 'APPROVED'");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("ALTER TABLE doctors ADD COLUMN phone VARCHAR(20) NULL");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN phone VARCHAR(20) NULL");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("UPDATE users u JOIN patients p ON u.id = p.user_id SET u.phone = p.phone WHERE u.phone IS NULL AND p.phone IS NOT NULL");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("UPDATE users u JOIN doctors d ON u.id = d.user_id SET u.phone = d.phone WHERE u.phone IS NULL AND d.phone IS NOT NULL");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("UPDATE users SET phone = '9876543210' WHERE email = 'admin@clinic.com' AND phone IS NULL");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("UPDATE users SET email_verified = TRUE WHERE email_verified IS NULL OR email IN ('admin@clinic.com', 'dr.ramesh@clinic.com', 'dr.priya@clinic.com', 'dr.amit@clinic.com', 'patient@clinic.com', 'hospital1@clinic.com', 'hospital2@clinic.com', 'hospital3@clinic.com')");
        } catch (Exception ignored) {}
        // 1. Seed Specializations
        Specialization genMed = getOrCreateSpecialization("General Medicine", "Primary healthcare and general adult medical conditions");
        Specialization cardio = getOrCreateSpecialization("Cardiology", "Heart health, blood pressure, and cardiovascular care");
        Specialization derma = getOrCreateSpecialization("Dermatology", "Skin, hair, nails, and cosmetic consultation");
        Specialization pedia = getOrCreateSpecialization("Pediatrics", "Infant, child, and adolescent healthcare");
        Specialization ortho = getOrCreateSpecialization("Orthopedics", "Bone, joint, spine, and musculoskeletal disorders");

        // 2. Seed Admin
        if (!userRepository.existsByEmail("admin@clinic.com")) {
            User admin = new User("Clinic Administrator", "admin@clinic.com", passwordEncoder.encode("admin123"), Role.ADMIN);
            userRepository.save(admin);
            System.out.println(">> Seeded Admin: admin@clinic.com / admin123");
        }

        // 3. Seed Doctor 1 (Dr. Ramesh Kumar - Cardiology) with default schedule (9 AM - 10 PM)
        if (!userRepository.existsByEmail("dr.ramesh@clinic.com")) {
            User docUser1 = new User("Dr. Ramesh Kumar", "dr.ramesh@clinic.com", passwordEncoder.encode("doctor123"), Role.DOCTOR);
            userRepository.save(docUser1);
            Doctor doc1 = new Doctor(docUser1, cardio, new BigDecimal("500.00"), true);
            doctorRepository.save(doc1);

            DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY};
            for (DayOfWeek day : days) {
                availabilityRepository.save(new DoctorAvailability(doc1, day, LocalTime.of(9, 0), LocalTime.of(22, 0)));
            }
            System.out.println(">> Seeded Doctor 1: dr.ramesh@clinic.com / doctor123 (9 AM - 10 PM)");
        }

        // 4. Seed Doctor 2 (Dr. Priya Sharma - Pediatrics) with customized schedule (10 AM - 2 PM)
        if (!userRepository.existsByEmail("dr.priya@clinic.com")) {
            User docUser2 = new User("Dr. Priya Sharma", "dr.priya@clinic.com", passwordEncoder.encode("doctor123"), Role.DOCTOR);
            userRepository.save(docUser2);
            Doctor doc2 = new Doctor(docUser2, pedia, new BigDecimal("400.00"), true);
            doctorRepository.save(doc2);

            DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY};
            for (DayOfWeek day : days) {
                availabilityRepository.save(new DoctorAvailability(doc2, day, LocalTime.of(10, 0), LocalTime.of(14, 0)));
            }
            System.out.println(">> Seeded Doctor 2: dr.priya@clinic.com / doctor123 (Custom: 10 AM - 2 PM)");
        }

        // 5. Seed Doctor 3 (Dr. Amit Patel - General Medicine) with default schedule (9 AM - 10 PM)
        if (!userRepository.existsByEmail("dr.amit@clinic.com")) {
            User docUser3 = new User("Dr. Amit Patel", "dr.amit@clinic.com", passwordEncoder.encode("doctor123"), Role.DOCTOR);
            userRepository.save(docUser3);
            Doctor doc3 = new Doctor(docUser3, genMed, new BigDecimal("300.00"), true);
            doctorRepository.save(doc3);

            DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY};
            for (DayOfWeek day : days) {
                availabilityRepository.save(new DoctorAvailability(doc3, day, LocalTime.of(9, 0), LocalTime.of(22, 0)));
            }
            System.out.println(">> Seeded Doctor 3: dr.amit@clinic.com / doctor123 (9 AM - 10 PM)");
        }

        // Safely migrate previously seeded default schedules (9:00 - 13:00) to 22:00 (10:00 PM), preserving custom schedules
        try {
            int updated = jdbcTemplate.update(
                "UPDATE doctor_availability SET end_time = '22:00:00' WHERE start_time = '09:00:00' AND end_time = '13:00:00'"
            );
            if (updated > 0) {
                System.out.println(">> Safely updated " + updated + " default availability records to 10:00 PM end time.");
            }
        } catch (Exception e) {
            System.out.println(">> Availability migration notice: " + e.getMessage());
        }

        // 6. Seed Sample Patient
        if (!userRepository.existsByEmail("patient@clinic.com")) {
            User patUser = new User("Rohan Sharma", "patient@clinic.com", passwordEncoder.encode("patient123"), Role.PATIENT);
            userRepository.save(patUser);
            Patient patient = new Patient(patUser, "9876543210", LocalDate.of(1998, 5, 15), "Male", "24 MG Road, Bangalore");
            patientRepository.save(patient);
            System.out.println(">> Seeded Patient: patient@clinic.com / patient123");
        }

        // 7. Seed Participating Hospitals & Ambulances
        if (!userRepository.existsByEmail("hospital1@clinic.com")) {
            User hUser1 = new User("City Central Emergency Hospital", "hospital1@clinic.com", passwordEncoder.encode("hospital123"), Role.HOSPITAL);
            userRepository.save(hUser1);
            Hospital h1 = new Hospital(hUser1, "City Central Emergency Hospital", "12 Richmond Road, Bangalore", "Richmond Town", "080-22211001", 12.9716, 77.5946, 20.0);
            hospitalRepository.save(h1);

            ambulanceRepository.save(new Ambulance(h1, "KA-01-EA-1001", "Suresh Kumar", "9871100101"));
            ambulanceRepository.save(new Ambulance(h1, "KA-01-EA-1002", "Manish Verma", "9871100102"));
            System.out.println(">> Seeded Hospital 1: hospital1@clinic.com / hospital123");
        }

        if (!userRepository.existsByEmail("hospital2@clinic.com")) {
            User hUser2 = new User("Apollo Lifeline Hospital", "hospital2@clinic.com", passwordEncoder.encode("hospital123"), Role.HOSPITAL);
            userRepository.save(hUser2);
            Hospital h2 = new Hospital(hUser2, "Apollo Lifeline Hospital", "154/11 Bannerghatta Road, Bangalore", "Bannerghatta", "080-26304000", 12.9279, 77.6271, 25.0);
            hospitalRepository.save(h2);

            ambulanceRepository.save(new Ambulance(h2, "KA-05-EM-2001", "Rajesh Gowda", "9872200201"));
            ambulanceRepository.save(new Ambulance(h2, "KA-05-EM-2002", "Deepak Rao", "9872200202"));
            System.out.println(">> Seeded Hospital 2: hospital2@clinic.com / hospital123");
        }

        if (!userRepository.existsByEmail("hospital3@clinic.com")) {
            User hUser3 = new User("Fortis Care Hospital", "hospital3@clinic.com", passwordEncoder.encode("hospital123"), Role.HOSPITAL);
            userRepository.save(hUser3);
            Hospital h3 = new Hospital(hUser3, "Fortis Care Hospital", "14 Cunningham Road, Bangalore", "Cunningham Road", "080-41994444", 12.9784, 77.6408, 20.0);
            hospitalRepository.save(h3);

            ambulanceRepository.save(new Ambulance(h3, "KA-03-FC-3001", "Anand Babu", "9873300301"));
            System.out.println(">> Seeded Hospital 3: hospital3@clinic.com / hospital123");
        }
    }

    private Specialization getOrCreateSpecialization(String name, String description) {
        return specializationRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> specializationRepository.save(new Specialization(name, description)));
    }
}
