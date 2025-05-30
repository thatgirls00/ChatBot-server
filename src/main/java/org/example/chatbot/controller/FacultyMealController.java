package org.example.chatbot.controller;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.FacultyMeal;
import org.example.chatbot.repository.FacultyMealRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faculty-meals")
@RequiredArgsConstructor
public class FacultyMealController {

    private final FacultyMealRepository repository;

    @GetMapping("/search")
    public ResponseEntity<List<FacultyMeal>> searchMeals(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String keyword) {

        List<FacultyMeal> result;

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