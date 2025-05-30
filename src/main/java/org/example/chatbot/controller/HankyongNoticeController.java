package org.example.chatbot.controller;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.HankyongNotice;
import org.example.chatbot.service.HankyongNoticeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hankyong-notices")
@RequiredArgsConstructor
public class HankyongNoticeController {

    private final HankyongNoticeService service;

    @GetMapping("/search")
    public ResponseEntity<List<HankyongNotice>> searchNotices(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String title) {

        List<HankyongNotice> result = service.searchNotices(date, title);
        return ResponseEntity.ok(result);
    }
}