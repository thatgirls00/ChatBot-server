package org.example.chatbot.controller;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.AcademicSchedule;
import org.example.chatbot.repository.AcademicScheduleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academic-schedule")
@RequiredArgsConstructor
public class AcademicScheduleController {

    private final AcademicScheduleRepository repository;

    @GetMapping("/search")
    public ResponseEntity<List<AcademicSchedule>> searchSchedules(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String keyword) {

        List<AcademicSchedule> result;

        if (date != null && keyword != null) {
            result = repository.findByDateContainingAndContentContaining(date, keyword);
        } else if (date != null) {
            result = repository.findByDateContaining(date);
        } else if (keyword != null) {
            result = repository.findByContentContaining(keyword);
        } else {
            result = repository.findAll();
        }

        return ResponseEntity.ok(result);
    }
}