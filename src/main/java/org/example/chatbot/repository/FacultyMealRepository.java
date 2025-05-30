package org.example.chatbot.repository;

import org.example.chatbot.domain.FacultyMeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacultyMealRepository extends JpaRepository<FacultyMeal, Long> {
    List<FacultyMeal> findByMealDateContainingAndMenuContaining(String mealDate, String keyword);
    List<FacultyMeal> findByMealDateContaining(String mealDate);
    List<FacultyMeal> findByMenuContaining(String keyword);
}