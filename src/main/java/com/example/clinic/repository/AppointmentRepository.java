package com.example.clinic.repository;

import com.example.clinic.entity.Appointment;
import com.example.clinic.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Check if slot is booked by active status (BOOKED or IN_CONSULTATION)
    boolean existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusIn(
            Long doctorId, LocalDate appointmentDate, LocalTime appointmentTime, Collection<AppointmentStatus> statuses);

    // Get all booked slots for a doctor on a date
    List<Appointment> findByDoctorIdAndAppointmentDateAndStatusIn(
            Long doctorId, LocalDate appointmentDate, Collection<AppointmentStatus> statuses);

    // Calculate total tokens booked for doctor on that date to assign next token
    int countByDoctorIdAndAppointmentDate(Long doctorId, LocalDate appointmentDate);

    // Patient's appointment history
    List<Appointment> findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(Long patientId);

    // Doctor's appointments for a specific date
    List<Appointment> findByDoctorIdAndAppointmentDateOrderByTokenNumberAsc(Long doctorId, LocalDate appointmentDate);

    // Doctor's appointments filtered by status
    List<Appointment> findByDoctorIdAndAppointmentDateAndStatusOrderByTokenNumberAsc(
            Long doctorId, LocalDate appointmentDate, AppointmentStatus status);

    // Doctor's current appointment in consultation
    Optional<Appointment> findFirstByDoctorIdAndAppointmentDateAndStatus(
            Long doctorId, LocalDate appointmentDate, AppointmentStatus status);

    // Doctor's next waiting appointment
    Optional<Appointment> findFirstByDoctorIdAndAppointmentDateAndStatusOrderByTokenNumberAsc(
            Long doctorId, LocalDate appointmentDate, AppointmentStatus status);
}
