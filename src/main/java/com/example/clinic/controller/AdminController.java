package com.example.clinic.controller;

import com.example.clinic.dto.*;
import com.example.clinic.entity.Specialization;
import com.example.clinic.service.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        AdminStatsResponse stats = adminService.getStats();
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/doctors")
    public ResponseEntity<DoctorDto> createDoctor(@RequestBody DoctorCreateRequest request) {
        DoctorDto created = adminService.createDoctor(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/doctors/{id}")
    public ResponseEntity<DoctorDto> updateDoctor(@PathVariable Long id, @RequestBody DoctorUpdateRequest request) {
        DoctorDto updated = adminService.updateDoctor(id, request);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/specializations")
    public ResponseEntity<List<Specialization>> getAllSpecializations() {
        List<Specialization> list = adminService.getAllSpecializations();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/specializations")
    public ResponseEntity<Specialization> createSpecialization(@RequestBody Specialization specialization) {
        Specialization created = adminService.createSpecialization(specialization);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<AppointmentResponse>> getAllAppointments() {
        List<AppointmentResponse> list = adminService.getAllAppointments();
        return ResponseEntity.ok(list);
    }
}
