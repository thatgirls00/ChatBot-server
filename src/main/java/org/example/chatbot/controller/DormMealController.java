package org.example.chatbot.controller;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.DormMeal;
import org.example.chatbot.repository.DormMealRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dorm-meals")
@RequiredArgsConstructor
public class DormMealController {

    private final DormMealRepository repository;

    @GetMapping("/search")
    public ResponseEntity<List<DormMeal>> searchMeals(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String keyword) {

        List<DormMeal> result;

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