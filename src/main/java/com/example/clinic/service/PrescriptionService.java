package com.example.clinic.service;

import com.example.clinic.dto.PrescriptionMedicineDto;
import com.example.clinic.dto.PrescriptionRequest;
import com.example.clinic.dto.PrescriptionResponse;
import com.example.clinic.entity.*;
import com.example.clinic.exception.BadRequestException;
import com.example.clinic.exception.ResourceNotFoundException;
import com.example.clinic.repository.AppointmentRepository;
import com.example.clinic.repository.PrescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final AppointmentRepository appointmentRepository;

    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               AppointmentRepository appointmentRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional
    public PrescriptionResponse createPrescription(PrescriptionRequest request) {
        // 1. Validate Appointment
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + request.getAppointmentId()));

        if (prescriptionRepository.existsByAppointmentId(appointment.getId())) {
            throw new BadRequestException("Prescription has already been created for this appointment.");
        }

        // 2. Create Prescription
        Prescription prescription = new Prescription();
        prescription.setAppointment(appointment);
        prescription.setDiagnosis(request.getDiagnosis());
        prescription.setNotes(request.getNotes());

        // 3. Add Medicines
        if (request.getMedicines() != null) {
            for (PrescriptionMedicineDto mDto : request.getMedicines()) {
                if (mDto.getMedicineName() != null && !mDto.getMedicineName().trim().isEmpty()) {
                    PrescriptionMedicine medicine = new PrescriptionMedicine(
                            mDto.getMedicineName().trim(),
                            mDto.getDosage(),
                            mDto.getFrequency(),
                            mDto.getDuration(),
                            mDto.getInstructions()
                    );
                    prescription.addMedicine(medicine);
                }
            }
        }

        Prescription saved = prescriptionRepository.save(prescription);

        // 4. Complete the appointment status
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);

        return mapToResponse(saved);
    }

    public PrescriptionResponse getPrescriptionByAppointmentId(Long appointmentId) {
        Prescription prescription = prescriptionRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("No prescription found for appointment ID: " + appointmentId));
        return mapToResponse(prescription);
    }

    public PrescriptionResponse getPrescriptionById(Long prescriptionId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with ID: " + prescriptionId));
        return mapToResponse(prescription);
    }

    private PrescriptionResponse mapToResponse(Prescription p) {
        PrescriptionResponse res = new PrescriptionResponse();
        res.setId(p.getId());
        res.setAppointmentId(p.getAppointment().getId());
        res.setDoctorName(p.getAppointment().getDoctor().getUser().getName());
        res.setDoctorSpecialization(p.getAppointment().getDoctor().getSpecialization().getName());
        res.setPatientName(p.getAppointment().getPatient().getUser().getName());
        res.setPatientPhone(p.getAppointment().getPatient().getPhone());
        res.setPatientGender(p.getAppointment().getPatient().getGender());
        res.setAppointmentDate(p.getAppointment().getAppointmentDate());
        res.setDiagnosis(p.getDiagnosis());
        res.setNotes(p.getNotes());
        res.setCreatedAt(p.getCreatedAt());

        List<PrescriptionMedicineDto> medDtos = p.getMedicines().stream()
                .map(m -> new PrescriptionMedicineDto(
                        m.getMedicineName(),
                        m.getDosage(),
                        m.getFrequency(),
                        m.getDuration(),
                        m.getInstructions()
                ))
                .collect(Collectors.toList());
        res.setMedicines(medDtos);

        return res;
    }
}
