package com.example.clinic.controller;

import com.example.clinic.dto.DoctorDto;
import com.example.clinic.dto.DoctorSlotsResponse;
import com.example.clinic.service.DoctorService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@CrossOrigin(origins = "*")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping
    public ResponseEntity<List<DoctorDto>> getAllDoctors(
            @RequestParam(required = false) Long specializationId,
            @RequestParam(required = false) String search) {
        List<DoctorDto> list = doctorService.getAllDoctors(specializationId, search);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoctorDto> getDoctorById(@PathVariable Long id) {
        DoctorDto doctor = doctorService.getDoctorById(id);
        return ResponseEntity.ok(doctor);
    }

    @GetMapping("/{id}/slots")
    public ResponseEntity<DoctorSlotsResponse> getDoctorSlots(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DoctorSlotsResponse response = doctorService.getDoctorSlots(id, date);
        return ResponseEntity.ok(response);
    }
}
