package org.example.chatbot.controller;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.StudentMeal;
import org.example.chatbot.repository.StudentMealRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student-meals")
@RequiredArgsConstructor
public class StudentMealController {

    private final StudentMealRepository repository;

    @GetMapping("/search")
    public ResponseEntity<List<StudentMeal>> searchMeals(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String keyword) {

        List<StudentMeal> result;

        if (date != null && keyword != null) {
            result = repository.findByMealDateContainingAndMenuContaining(date, keyword);
        } else if (date != null) {
            result = repository.findByMealDateContaining(date);
        } else if (keyword != null) {
            result = repository.findByMenuContaining(keyword);
        } else {
            result = repository.findAll();
        }

        return ResponseEntity.ok(result);
    }
}
