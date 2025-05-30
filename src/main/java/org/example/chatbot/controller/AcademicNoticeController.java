package org.example.chatbot.controller;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.AcademicNotice;
import org.example.chatbot.repository.AcademicNoticeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academic-notices")
@RequiredArgsConstructor
public class AcademicNoticeController {

    private final AcademicNoticeRepository repository;

    @GetMapping("/search")
    public ResponseEntity<List<AcademicNotice>> searchNotices(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String keyword) {

        List<AcademicNotice> result;

        if (date != null && keyword != null) {
            result = repository.findByNoticeDateContainingAndTitleContaining(date, keyword);
        } else if (date != null) {
            result = repository.findByNoticeDateContaining(date);
        } else if (keyword != null) {
            result = repository.findByTitleContaining(keyword);
        } else {
            result = repository.findAll();
        }

        return ResponseEntity.ok(result);
    }
}