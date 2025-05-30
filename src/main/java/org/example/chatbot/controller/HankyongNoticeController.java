package org.example.chatbot.controller;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.HankyongNotice;
import org.example.chatbot.repository.HankyongNoticeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hankyong-notices")
@RequiredArgsConstructor
public class HankyongNoticeController {

    private final HankyongNoticeRepository repository;

    @GetMapping("/search")
    public ResponseEntity<List<HankyongNotice>> searchNotices(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String title) {

        List<HankyongNotice> result;

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