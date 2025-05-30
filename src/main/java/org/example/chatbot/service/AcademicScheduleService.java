package org.example.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.AcademicSchedule;
import org.example.chatbot.repository.AcademicScheduleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AcademicScheduleService {

    private final AcademicScheduleRepository repository;

    public List<AcademicSchedule> searchSchedules(String date, String keyword) {
        if (date != null && keyword != null) {
            return repository.findByDateContainingAndContentContaining(date, keyword);
        } else if (date != null) {
            return repository.findByDateContaining(date);
        } else if (keyword != null) {
            return repository.findByContentContaining(keyword);
        } else {
            return repository.findAll();
        }
    }
}