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

    /**
     * 사용자 질문에서 intent, date, mealTime, keyword를 GPT로부터 JSON 형태로 받아 파싱.
     */
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

            String intent   = root.has("intent") ? root.get("intent").asText(null) : null;
            String date     = root.has("date") ? root.get("date").asText(null) : null;
            String mealTime = root.has("mealTime") ? root.get("mealTime").asText(null) : null;
            String keyword  = root.has("keyword") ? root.get("keyword").asText(null) : null;

            if (userInput.contains("일정")) {
                log.error("📥 질문에 '일정' 키워드 감지, intent를 '학사일정'으로 강제 지정합니다.");
                return new IntentResultDto("학사일정", date, keyword, mealTime, null);
            }

            // GPT가 intent를 못 잡거나 유효하지 않은 intent를 반환하면 직접 보정
            if ((intent == null || !VALID_INTENTS.contains(intent))) {
                // 질문에 "식당" 키워드가 있으면 식당 intent로 유도
                if (userInput.contains("식당")) {
                    log.error("📥 식당 키워드 기반으로 intent를 '식당 미지정'으로 보정합니다.");
                    return new IntentResultDto("식당 미지정", null, null, null,
                            "어느 식당의 식단이 궁금하신가요? 학생식당, 교직원식당, 기숙사식당 중 선택해 주세요.");
                }
                // VALID_INTENTS에 속하지 않으면 fallback
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

    /**
     * intent가 "없음"일 때 친화적인 fallback 답변 생성.
     */
    public String generateFallbackAnswer(String userInput) {
        String prompt = GptPromptBuilder.buildFallbackPrompt(userInput);
        return sendToGpt(prompt);
    }

    /**
     * 기숙사 식단 메뉴 원본을 사람이 읽기 좋게 포맷팅.
     */
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

    /**
     * GPT API에 요청을 보내고 응답을 받음.
     */
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

    /**
     * GPT 응답 JSON에서 메시지 content를 꺼냄.
     */
    @SuppressWarnings("unchecked")
    private String extractContentFromResponse(ResponseEntity<Map> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return message.get("content").toString().trim();
    }

    /**
     * GPT 응답 문자열에 포함될 수 있는 BOM, 제어문자, 불필요한 공백 제거.
     */
    private String sanitizeGptResponse(String content) {
        return content.replaceAll("[\\u0000-\\u001F\\u007F\\uFEFF-\\uFFFF]", "").trim();
    }
}