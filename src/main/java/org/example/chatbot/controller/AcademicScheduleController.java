package org.example.chatbot.controller;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.AcademicSchedule;
import org.example.chatbot.service.AcademicScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academic-schedule")
@RequiredArgsConstructor
public class AcademicScheduleController {

    private final AcademicScheduleService service;

    @GetMapping("/search")
    public ResponseEntity<List<AcademicSchedule>> searchSchedules(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String keyword) {

        List<AcademicSchedule> result = service.searchSchedules(date, keyword);
        return ResponseEntity.ok(result);
    }
}