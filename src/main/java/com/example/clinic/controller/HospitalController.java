package com.example.clinic.controller;

import com.example.clinic.dto.HospitalDto;
import com.example.clinic.entity.Ambulance;
import com.example.clinic.entity.Doctor;
import com.example.clinic.service.HospitalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hospitals")
@CrossOrigin(origins = "*")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @GetMapping
    public ResponseEntity<List<HospitalDto>> getAllHospitals() {
        return ResponseEntity.ok(hospitalService.getAllActiveHospitals());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HospitalDto> getHospitalById(@PathVariable Long id) {
        return ResponseEntity.ok(hospitalService.getHospitalById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<HospitalDto> getHospitalByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(hospitalService.getHospitalByUserId(userId));
    }

    @GetMapping("/{id}/ambulances")
    public ResponseEntity<List<Ambulance>> getAmbulances(@PathVariable Long id) {
        return ResponseEntity.ok(hospitalService.getAmbulancesForHospital(id));
    }

    @GetMapping("/{id}/ambulances/available")
    public ResponseEntity<List<Ambulance>> getAvailableAmbulances(@PathVariable Long id) {
        return ResponseEntity.ok(hospitalService.getAvailableAmbulancesForHospital(id));
    }

    @GetMapping("/doctors/available")
    public ResponseEntity<List<Doctor>> getAvailableDoctors() {
        return ResponseEntity.ok(hospitalService.getAvailableDoctors());
    }
}
