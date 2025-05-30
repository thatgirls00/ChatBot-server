package org.example.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.AcademicNotice;
import org.example.chatbot.repository.AcademicNoticeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AcademicNoticeService {

    private final AcademicNoticeRepository repository;

    public List<AcademicNotice> searchNotices(String date, String keyword) {
        if (date != null && keyword != null) {
            return repository.findByNoticeDateContainingAndTitleContaining(date, keyword);
        } else if (date != null) {
            return repository.findByNoticeDateContaining(date);
        } else if (keyword != null) {
            return repository.findByTitleContaining(keyword);
        } else {
            return repository.findAll();
        }
    }
}