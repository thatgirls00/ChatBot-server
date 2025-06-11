package org.example.chatbot.repository;

import org.example.chatbot.domain.DormMeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DormMealRepository extends JpaRepository<DormMeal, Long> {

    List<DormMeal> findByMealDateContainingAndMenuContaining(String mealDate, String keyword);

    List<DormMeal> findByMealDateContaining(String mealDate);

    List<DormMeal> findByMenuContaining(String keyword);
}