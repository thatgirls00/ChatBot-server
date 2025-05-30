package org.example.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.StudentMeal;
import org.example.chatbot.repository.StudentMealRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentMealService {

    private final StudentMealRepository repository;

    public List<StudentMeal> searchMeals(String date, String keyword) {
        if (date != null && keyword != null) {
            return repository.findByMealDateContainingAndMenuContaining(date, keyword);
        } else if (date != null) {
            return repository.findByMealDateContaining(date);
        } else if (keyword != null) {
            return repository.findByMenuContaining(keyword);
        } else {
            return repository.findAll();
        }
    }
}