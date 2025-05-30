package org.example.chatbot.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hankyong_notices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HankyongNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String noticeDate;
    private String author;
    @Column(columnDefinition = "TEXT")
    private String link;
    @Column(length = 64, unique = true)
    private String hash;
}