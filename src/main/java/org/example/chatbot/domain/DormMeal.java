package org.example.chatbot.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dorm_meals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DormMeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mealDate;
    private String mealTime;
    @Column(columnDefinition = "TEXT")
    private String menu;
    @Column(length = 64, unique = true)
    private String hash;
}