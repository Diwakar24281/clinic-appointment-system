package com.example.clinic.controller;

import com.example.clinic.dto.AppointmentResponse;
import com.example.clinic.dto.QueueStatusResponse;
import com.example.clinic.service.QueueService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/queue")
@CrossOrigin(origins = "*")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<QueueStatusResponse> getQueueStatus(
            @PathVariable Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        QueueStatusResponse response = queueService.getQueueStatus(doctorId, date);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/doctor/{doctorId}/call-next")
    public ResponseEntity<AppointmentResponse> callNextPatient(@PathVariable Long doctorId) {
        AppointmentResponse response = queueService.callNextPatient(doctorId);
        return ResponseEntity.ok(response);
    }
}
