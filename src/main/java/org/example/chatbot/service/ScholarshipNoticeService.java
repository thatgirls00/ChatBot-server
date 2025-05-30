package org.example.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.ScholarshipNotice;
import org.example.chatbot.repository.ScholarshipNoticeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScholarshipNoticeService {

    private final ScholarshipNoticeRepository repository;

    public List<ScholarshipNotice> searchNotices(String date, String title) {
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