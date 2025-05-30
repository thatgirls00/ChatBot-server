package org.example.chatbot.controller;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.ScholarshipNotice;
import org.example.chatbot.service.ScholarshipNoticeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scholarship-notices")
@RequiredArgsConstructor
public class ScholarshipNoticeController {

    private final ScholarshipNoticeService service;

    @GetMapping("/search")
    public ResponseEntity<List<ScholarshipNotice>> searchNotices(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String title) {

        List<ScholarshipNotice> result = service.searchNotices(date, title);
        return ResponseEntity.ok(result);
    }
}