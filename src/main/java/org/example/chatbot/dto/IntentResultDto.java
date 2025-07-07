package org.example.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IntentResultDto {

    private String intent;
    private String date;
    private String keyword;
    private String mealTime;
    private String answer;
}