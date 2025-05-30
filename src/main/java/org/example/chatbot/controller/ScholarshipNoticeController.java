package org.example.chatbot.controller;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.ScholarshipNotice;
import org.example.chatbot.repository.ScholarshipNoticeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scholarship-notices")
@RequiredArgsConstructor
public class ScholarshipNoticeController {

    private final ScholarshipNoticeRepository repository;

    @GetMapping("/search")
    public ResponseEntity<List<ScholarshipNotice>> searchNotices(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String title) {

        List<ScholarshipNotice> result;

        if (date != null && title != null) {
            result = repository.findByNoticeDateContainingAndTitleContaining(date, title);
        } else if (date != null) {
            result = repository.findByNoticeDateContaining(date);
        } else if (title != null) {
            result = repository.findByTitleContaining(title);
        } else {
            result = repository.findAll();
        }

        return ResponseEntity.ok(result);
    }
}