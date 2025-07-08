package org.example.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chatbot.dto.IntentResultDto;
import org.example.chatbot.util.GptPromptBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GptService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.api.model}")
    private String model;

    private static final Set<String> VALID_INTENTS = Set.of(
            "학생식당", "교직원식당", "기숙사식당",
            "학사공지", "장학공지", "학사일정", "한경공지",
            "식당 미지정"
    );

    public IntentResultDto classifyIntent(String userInput) {
        String prompt = GptPromptBuilder.buildIntentAndKeywordPrompt(userInput);
        String rawContent = sendToGpt(prompt);
        String content = sanitizeGptResponse(rawContent).trim();

        log.error("📥 GPT 원문 응답(raw): {}", rawContent);
        log.error("📥 GPT 정리된 응답(sanitized): {}", content);

        try {
            if (!content.startsWith("{")) {
                log.error("❗ GPT가 JSON이 아닌 응답을 반환했습니다. fallback 안내 사용");
                return handleIntentFallback(userInput, content);
            }

            JsonNode root = objectMapper.readTree(content);

            String intent    = root.has("intent") ? root.get("intent").asText(null) : null;
            String rawDate   = root.has("date") ? root.get("date").asText(null) : null;
            String date      = correctToCurrentYear(rawDate);
            String mealTime  = root.has("mealTime") ? root.get("mealTime").asText(null) : null;
            String keyword   = root.has("keyword") ? root.get("keyword").asText(null) : null;

            if (userInput.contains("일정")) {
                log.error("📥 질문에 '일정' 키워드 감지, intent를 '학사일정'으로 강제 지정합니다.");
                return new IntentResultDto("학사일정", date, keyword, mealTime, null);
            }

            if ((intent == null || !VALID_INTENTS.contains(intent))) {
                if (userInput.contains("식당")) {
                    log.error("📥 식당 키워드 기반으로 intent를 '식당 미지정'으로 보정합니다.");
                    return new IntentResultDto("식당 미지정", null, null, null,
                            "어느 식당의 식단이 궁금하신가요? 학생식당, 교직원식당, 기숙사식당 중 선택해 주세요.");
                }
                return new IntentResultDto("없음", null, null, null, null);
            }

            return new IntentResultDto(intent, date, keyword, mealTime, null);

        } catch (Exception e) {
            log.error("❗ GPT 응답 JSON 파싱 실패: {}", e.getMessage());
            log.error("📥 파싱 실패 시 원문: {}", content);
            return handleIntentFallback(userInput, content);
        }
    }

    private IntentResultDto handleIntentFallback(String userInput, String content) {
        if (userInput.contains("식당")) {
            log.error("📥 fallback에서도 식당 키워드로 intent를 '식당 미지정'으로 보정합니다.");
            return new IntentResultDto("식당 미지정", null, null, null,
                    "어느 식당의 식단이 궁금하신가요? 학생식당, 교직원식당, 기숙사식당 중 선택해 주세요.");
        }
        return new IntentResultDto("없음", null, null, null, content);
    }

    public String generateFallbackAnswer(String userInput) {
        String prompt = GptPromptBuilder.buildFallbackPrompt(userInput);
        return sendToGpt(prompt);
    }

    public String formatMealWithGpt(String rawMenu) {
        String prompt = String.format(
                """
                아래 기숙사 식단 메뉴를 시간대별로 [아침], [점심], [저녁] 태그를 붙여 구분하고, 각 항목은 - 기호로 줄바꿈해 깔끔하게 출력해줘.
                다른 텍스트는 절대 추가하지 말고, 메뉴 내용만 다음 예시와 같은 형태로 반환해:

[점심] 12:00~13:00
- 귀리밥
- 소고기무국 (호주산)
...

[저녁] 17:00~18:10
- 참치김치밥
...

아래는 메뉴 원본이다:
%s
                """,
                rawMenu
        );
        return sendToGpt(prompt).trim();
    }

    private String sendToGpt(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.0,
                "max_tokens", 500,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
            return extractContentFromResponse(response);
        } catch (Exception e) {
            log.error("❗ GPT 호출 실패: {}", e.getMessage());
            return "메뉴 포맷팅에 실패했습니다.";
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContentFromResponse(ResponseEntity<Map> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return message.get("content").toString().trim();
    }

    private String sanitizeGptResponse(String content) {
        return content.replaceAll("[\\u0000-\\u001F\\u007F\\uFEFF-\\uFFFF]", "").trim();
    }

    /**
     * GPT가 반환한 date 문자열이 과거 연도이면 현재 연도로 보정
     */
    private String correctToCurrentYear(String dateStr) {
        if (dateStr == null) return null;
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        try {
            // YYYY
            if (dateStr.matches("^\\d{4}$")) {
                int year = Integer.parseInt(dateStr);
                if (year < currentYear) {
                    log.warn("📌 연도 보정: '{}' → '{}'", dateStr, currentYear);
                    return String.valueOf(currentYear);
                }
            }
            // YYYY-MM
            else if (dateStr.matches("^\\d{4}-\\d{2}$")) {
                int year = Integer.parseInt(dateStr.substring(0, 4));
                if (year < currentYear) {
                    String corrected = currentYear + dateStr.substring(4);
                    log.warn("📌 연도 보정: '{}' → '{}'", dateStr, corrected);
                    return corrected;
                }
            }
            // YYYY-MM-DD
            else if (dateStr.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                int year = Integer.parseInt(dateStr.substring(0, 4));
                if (year < currentYear) {
                    String corrected = currentYear + dateStr.substring(4);
                    log.warn("📌 연도 보정: '{}' → '{}'", dateStr, corrected);
                    return corrected;
                }
            }
        } catch (Exception e) {
            log.error("❗ 연도 보정 실패: {}, 이유: {}", dateStr, e.getMessage());
        }

        return dateStr;
    }
}