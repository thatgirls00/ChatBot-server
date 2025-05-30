package org.example.chatbot.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "academic_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AcademicSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String date;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;
    @Column(length = 64, unique = true)
    private String hash;
}