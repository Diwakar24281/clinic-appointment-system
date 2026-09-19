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
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UserRepository userRepository,
                      SpecializationRepository specializationRepository,
                      DoctorRepository doctorRepository,
                      DoctorAvailabilityRepository availabilityRepository,
                      PatientRepository patientRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.specializationRepository = specializationRepository;
        this.doctorRepository = doctorRepository;
        this.availabilityRepository = availabilityRepository;
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
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

        // 3. Seed Doctor 1 (Dr. Ramesh Kumar - Cardiology)
        if (!userRepository.existsByEmail("dr.ramesh@clinic.com")) {
            User docUser1 = new User("Dr. Ramesh Kumar", "dr.ramesh@clinic.com", passwordEncoder.encode("doctor123"), Role.DOCTOR);
            userRepository.save(docUser1);
            Doctor doc1 = new Doctor(docUser1, cardio, new BigDecimal("500.00"), true);
            doctorRepository.save(doc1);

            DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY};
            for (DayOfWeek day : days) {
                availabilityRepository.save(new DoctorAvailability(doc1, day, LocalTime.of(9, 0), LocalTime.of(13, 0)));
            }
            System.out.println(">> Seeded Doctor 1: dr.ramesh@clinic.com / doctor123");
        }

        // 4. Seed Doctor 2 (Dr. Priya Sharma - Pediatrics)
        if (!userRepository.existsByEmail("dr.priya@clinic.com")) {
            User docUser2 = new User("Dr. Priya Sharma", "dr.priya@clinic.com", passwordEncoder.encode("doctor123"), Role.DOCTOR);
            userRepository.save(docUser2);
            Doctor doc2 = new Doctor(docUser2, pedia, new BigDecimal("400.00"), true);
            doctorRepository.save(doc2);

            DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY};
            for (DayOfWeek day : days) {
                availabilityRepository.save(new DoctorAvailability(doc2, day, LocalTime.of(10, 0), LocalTime.of(14, 0)));
            }
            System.out.println(">> Seeded Doctor 2: dr.priya@clinic.com / doctor123");
        }

        // 5. Seed Doctor 3 (Dr. Amit Patel - General Medicine)
        if (!userRepository.existsByEmail("dr.amit@clinic.com")) {
            User docUser3 = new User("Dr. Amit Patel", "dr.amit@clinic.com", passwordEncoder.encode("doctor123"), Role.DOCTOR);
            userRepository.save(docUser3);
            Doctor doc3 = new Doctor(docUser3, genMed, new BigDecimal("300.00"), true);
            doctorRepository.save(doc3);

            DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY};
            for (DayOfWeek day : days) {
                availabilityRepository.save(new DoctorAvailability(doc3, day, LocalTime.of(9, 0), LocalTime.of(13, 0)));
            }
            System.out.println(">> Seeded Doctor 3: dr.amit@clinic.com / doctor123");
        }

        // 6. Seed Sample Patient
        if (!userRepository.existsByEmail("patient@clinic.com")) {
            User patUser = new User("Rohan Sharma", "patient@clinic.com", passwordEncoder.encode("patient123"), Role.PATIENT);
            userRepository.save(patUser);
            Patient patient = new Patient(patUser, "9876543210", LocalDate.of(1998, 5, 15), "Male", "24 MG Road, Bangalore");
            patientRepository.save(patient);
            System.out.println(">> Seeded Patient: patient@clinic.com / patient123");
        }
    }

    private Specialization getOrCreateSpecialization(String name, String description) {
        return specializationRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> specializationRepository.save(new Specialization(name, description)));
    }
}
