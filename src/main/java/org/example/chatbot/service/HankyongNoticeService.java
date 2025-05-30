package org.example.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.HankyongNotice;
import org.example.chatbot.repository.HankyongNoticeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HankyongNoticeService {

    private final HankyongNoticeRepository repository;

    public List<HankyongNotice> searchNotices(String date, String title) {
        if (date != null && title != null) {
            return repository.findByNoticeDateContainingAndTitleContaining(date, title);
        } else if (date != null) {
            return repository.findByNoticeDateContaining(date);
        } else if (title != null) {
            return repository.findByTitleContaining(title);
        } else {
            return repository.findAll();
        }
    }
}