package org.example.chatbot.controller;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.DormMeal;
import org.example.chatbot.service.DormMealService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dorm-meals")
@RequiredArgsConstructor
public class DormMealController {

    private final DormMealService service;

    @GetMapping("/search")
    public ResponseEntity<List<DormMeal>> searchMeals(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String keyword) {

        List<DormMeal> result = service.searchMeals(date, keyword);
        return ResponseEntity.ok(result);
    }
}