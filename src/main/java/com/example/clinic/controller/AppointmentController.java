package com.example.clinic.controller;

import com.example.clinic.dto.ApiResponse;
import com.example.clinic.dto.AppointmentRequest;
import com.example.clinic.dto.AppointmentResponse;
import com.example.clinic.service.AppointmentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "*")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> bookAppointment(@RequestBody AppointmentRequest request) {
        AppointmentResponse response = appointmentService.bookAppointment(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getAppointmentById(@PathVariable Long id) {
        AppointmentResponse response = appointmentService.getAppointmentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<AppointmentResponse>> getPatientAppointments(@PathVariable Long patientId) {
        List<AppointmentResponse> list = appointmentService.getPatientAppointments(patientId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<AppointmentResponse>> getDoctorAppointments(
            @PathVariable Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AppointmentResponse> list = appointmentService.getDoctorAppointments(doctorId, date);
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse> cancelAppointment(@PathVariable Long id) {
        ApiResponse response = appointmentService.cancelAppointment(id);
        return ResponseEntity.ok(response);
    }
}
