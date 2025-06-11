package org.example.chatbot.repository;

import org.example.chatbot.domain.StudentMeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentMealRepository extends JpaRepository<StudentMeal, Long> {

    List<StudentMeal> findByMealDateContainingAndMenuContaining(String mealDate, String keyword);

    List<StudentMeal> findByMealDateContaining(String mealDate);

    List<StudentMeal> findByMenuContaining(String keyword);

}