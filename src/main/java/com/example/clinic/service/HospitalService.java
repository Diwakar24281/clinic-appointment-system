package com.example.clinic.service;

import com.example.clinic.dto.HospitalDto;
import com.example.clinic.entity.Ambulance;
import com.example.clinic.entity.Doctor;
import com.example.clinic.entity.Hospital;
import com.example.clinic.exception.ResourceNotFoundException;
import com.example.clinic.repository.AmbulanceRepository;
import com.example.clinic.repository.DoctorRepository;
import com.example.clinic.repository.HospitalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HospitalService {

    private final HospitalRepository hospitalRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final DoctorRepository doctorRepository;

    public HospitalService(HospitalRepository hospitalRepository,
                           AmbulanceRepository ambulanceRepository,
                           DoctorRepository doctorRepository) {
        this.hospitalRepository = hospitalRepository;
        this.ambulanceRepository = ambulanceRepository;
        this.doctorRepository = doctorRepository;
    }

    public List<HospitalDto> getAllActiveHospitals() {
        return hospitalRepository.findByIsActiveTrueAndHasEmergencyServicesTrue()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public HospitalDto getHospitalById(Long id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital not found with id: " + id));
        return mapToDto(hospital);
    }

    public HospitalDto getHospitalByUserId(Long userId) {
        Hospital hospital = hospitalRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No hospital associated with user id: " + userId));
        return mapToDto(hospital);
    }

    public List<Ambulance> getAmbulancesForHospital(Long hospitalId) {
        return ambulanceRepository.findByHospitalId(hospitalId);
    }

    public List<Ambulance> getAvailableAmbulancesForHospital(Long hospitalId) {
        return ambulanceRepository.findByHospitalIdAndIsAvailableTrue(hospitalId);
    }

    public List<Doctor> getAvailableDoctors() {
        return doctorRepository.findByIsAvailableTrue();
    }

    private HospitalDto mapToDto(Hospital hospital) {
        HospitalDto dto = new HospitalDto();
        dto.setId(hospital.getId());
        dto.setUserId(hospital.getUser().getId());
        dto.setName(hospital.getName());
        dto.setAddress(hospital.getAddress());
        dto.setArea(hospital.getArea());
        dto.setPhone(hospital.getPhone());
        dto.setLatitude(hospital.getLatitude());
        dto.setLongitude(hospital.getLongitude());
        dto.setServiceRadiusKm(hospital.getServiceRadiusKm());
        dto.setActive(hospital.isActive());
        dto.setHasEmergencyServices(hospital.isHasEmergencyServices());
        return dto;
    }
}
