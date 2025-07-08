package org.example.chatbot.controller;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.dto.GptRequestDto;
import org.example.chatbot.dto.GptResponseDto;
import org.example.chatbot.dto.IntentResultDto;
import org.example.chatbot.service.ChatSessionService;
import org.example.chatbot.service.GptService;
import org.example.chatbot.service.TableQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

import static org.example.chatbot.util.DateTimeExtractor.extractDateRange;
import static org.example.chatbot.util.DateTimeExtractor.extractMealTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class GptController {

    private final GptService gptService;
    private final TableQueryService tableQueryService;
    private final ChatSessionService chatSessionService;

    private static final Set<String> MEAL_INTENTS = Set.of("학생식당", "교직원식당", "기숙사식당");
    private static final Set<String> NOTICE_INTENTS = Set.of("학사공지", "장학공지", "한경공지");
    private static final String SCHEDULE_INTENT = "학사일정";

    @GetMapping("/intent")
    public ResponseEntity<GptResponseDto> getSession(@RequestParam("userId") String userId) {
        String lastIntent = chatSessionService.getLastIntent(userId);
        String lastDate = chatSessionService.getLastDate(userId);
        String lastKeyword = chatSessionService.getLastKeyword(userId);
        String lastMealTime = chatSessionService.getLastMealTime(userId);

        if (lastIntent == null) {
            return ResponseEntity.ok(new GptResponseDto(
                    null,
                    "안녕하세요! 한경국립대학교 챗봇입니다. \n학사공지, 학사일정, 식단 등을 편하게 물어보세요. 예: '7월 학사일정 알려줘', '오늘 기숙사식당 메뉴 알려줘' 등"
            ));
        }

        String summary = String.format("이전에 '%s' 관련 질문을 하셨습니다. 이어서 질문해 보세요.", lastIntent);
        return ResponseEntity.ok(new GptResponseDto(lastIntent, summary));
    }

    @PostMapping("/intent")
    public ResponseEntity<GptResponseDto> handleUserInput(@RequestBody GptRequestDto request) {
        String userInput = request.getMessage();
        String userId = request.getUserId();

        IntentResultDto result = gptService.classifyIntent(userInput);
        String intent = result.getIntent();
        String keyword = result.getKeyword();
        String answer = result.getAnswer();
        String dateStr = result.getDate();

        LocalDate[] dateRange;
        LocalDate startDate, endDate;

        if (dateStr != null) {
            if (dateStr.matches("\\d{4}-\\d{2}")) { // YYYY-MM 형태면 월 범위 보정
                YearMonth ym = YearMonth.parse(dateStr);
                startDate = ym.atDay(1);
                endDate = ym.atEndOfMonth();
            } else {
                dateRange = extractDateRange(userInput);
                startDate = dateRange[0];
                endDate = dateRange[1];
            }
        } else {
            dateRange = extractDateRange(userInput);
            startDate = dateRange[0];
            endDate = dateRange[1];
        }

        // 이 아래에 null fallback 처리 추가
        if (startDate == null || endDate == null) {
            startDate = LocalDate.now();
            endDate = LocalDate.now();
        }

        boolean dateFilterApplied = !(startDate.equals(endDate) && startDate.equals(LocalDate.now()));
        if (containsDateKeyword(userInput)) dateFilterApplied = true;

        String mealTime = extractMealTime(userInput);
        if ("학생식당".equals(intent) && "점심".equals(mealTime)) mealTime = null;

        // intent가 식당 미지정이면 → 식당 선택 재질문
        if ("식당 미지정".equalsIgnoreCase(intent)) {
            return ResponseEntity.ok(new GptResponseDto(
                    "식당 미지정", "어느 식당의 식단이 궁금하신가요? 학생식당, 교직원식당, 기숙사식당 중 선택해 주세요."
            ));
        }

        // GPT가 intent 없이 안내 메시지만 준 경우 → fallback
        if ((intent == null || intent.trim().isEmpty()) && answer != null) {
            return ResponseEntity.ok(new GptResponseDto(null, answer));
        }

        String normalizedIntent = intent != null ? intent.trim() : "";

        // intent가 비어있으면 Redis에서 복원
        if (normalizedIntent.isEmpty() || "없음".equalsIgnoreCase(normalizedIntent)) {
            intent = chatSessionService.getLastIntent(userId);
            String savedDate = chatSessionService.getLastDate(userId);
            keyword = chatSessionService.getLastKeyword(userId);
            if (mealTime == null) mealTime = chatSessionService.getLastMealTime(userId);

            if (savedDate != null) startDate = LocalDate.parse(savedDate);
            endDate = startDate;
            dateFilterApplied = !(startDate.equals(endDate) && startDate.equals(LocalDate.now()));

            if (intent == null) {
                String fallback = gptService.generateFallbackAnswer(userInput);
                return ResponseEntity.ok(new GptResponseDto("없음", fallback));
            }
        }

        normalizedIntent = intent.trim();

        // intent가 식당 미지정인 경우 → 어느 식당인지 재질문
        if ("식당 미지정".equalsIgnoreCase(normalizedIntent)) {
            return ResponseEntity.ok(new GptResponseDto(
                    "식당 미지정", "어느 식당의 식단이 궁금하신가요? 학생식당, 교직원식당, 기숙사식당 중 선택해 주세요."
            ));
        }

        // 식당 intent지만 명확하지 않으면 재질문
        if (intent != null && intent.contains("식당") && !MEAL_INTENTS.contains(intent)) {
            return ResponseEntity.ok(new GptResponseDto(
                    "식당 미지정", "어느 식당의 식단이 궁금하신가요? 학생식당, 교직원식당, 기숙사식당 중 선택해 주세요."
            ));
        }

        // intent가 없음 처리
        if ("없음".equalsIgnoreCase(normalizedIntent)) {
            if (userInput.contains("공지")) {
                return ResponseEntity.ok(new GptResponseDto(
                        "없음", "학사공지, 장학공지, 한경공지 중에서 어떤 공지사항이 궁금하신가요?"
                ));
            }
            String fallback = gptService.generateFallbackAnswer(userInput);
            return ResponseEntity.ok(new GptResponseDto("없음", fallback));
        }

        // 식당 intent 명확 → 날짜 확인
        if (MEAL_INTENTS.contains(intent)) {
            if (!dateFilterApplied) {
                return ResponseEntity.ok(new GptResponseDto(
                        intent, "어느 날짜의 메뉴가 궁금하신가요? 예: 오늘, 내일, 7월 8일 등으로 입력해 주세요."
                ));
            }
            List<?> dataList = tableQueryService.findMealDataByIntent(intent, keyword);
            String mealAnswer = tableQueryService.filterMealByConditions(
                    intent, keyword, mealTime, startDate, endDate, dateFilterApplied, dataList);
            chatSessionService.saveSession(userId, intent, startDate.toString(), keyword, mealTime);
            return ResponseEntity.ok(new GptResponseDto(intent, mealAnswer));
        }

        // 공지/일정 intent → 키워드 누락 시 재질문 포함
        if (NOTICE_INTENTS.contains(intent) || SCHEDULE_INTENT.equals(intent)) {
            if ((keyword == null || keyword.isBlank())) {
                if (dateFilterApplied) {
                    System.out.println("키워드 누락 → date 기반으로 검색 진행");
                } else {
                    String reask = switch (intent) {
                        case "학사공지" -> "학사공지에서 어떤 내용을 찾으시나요? 예: 휴학, 등록금 등 키워드를 입력해 주세요.";
                        case "장학공지" -> "장학공지에서 어떤 내용을 찾으시나요? 예: 국가장학금, 교내장학금 등 키워드를 입력해 주세요.";
                        case "한경공지" -> "한경공지에서 어떤 내용을 찾으시나요? 예: 행사, 모집 공고 등 키워드를 입력해 주세요.";
                        case "학사일정" -> "어떤 학사일정을 찾으시나요? 예: 수강신청, 휴학 등 키워드를 입력해 주세요.";
                        default -> "조금 더 구체적으로 어떤 정보를 찾으시는지 말씀해 주세요.";
                    };
                    return ResponseEntity.ok(new GptResponseDto(intent, reask));
                }
            }
        }

        GptResponseDto response;

        if (NOTICE_INTENTS.contains(intent)) {
            List<?> dataList = tableQueryService.findNoticeDataByIntent(intent, keyword);
            String noticeAnswer = tableQueryService.filterNoticeByConditions(
                    keyword, startDate, endDate, dateFilterApplied, dataList);
            response = new GptResponseDto(intent, noticeAnswer);

        } else if (SCHEDULE_INTENT.equals(intent)) {
            keyword = normalizeKeyword(keyword); // 졸업/학위수여 동의어 처리
            List<?> dataList = tableQueryService.findNoticeDataByIntent(intent, null);
            String scheduleAnswer;

            if (keyword != null && !keyword.isBlank()) {
                // keyword 우선 검색
                scheduleAnswer = tableQueryService.filterAcademicScheduleByConditions(
                        keyword, null, null, false, dataList);

                if (scheduleAnswer.isBlank()) {
                    // fallback: 다른 날짜에 있을 경우 안내
                    String otherDate = tableQueryService.findKeywordInOtherDates(keyword, startDate, endDate);
                    if (!otherDate.isBlank()) {
                        scheduleAnswer = String.format(
                                "요청하신 기간에는 '%s' 일정이 없지만, %s에 같은 일정이 있습니다.", keyword, otherDate);
                    } else {
                        scheduleAnswer = String.format(
                                "'%s' 키워드에 해당하는 학사일정을 찾을 수 없습니다.", keyword);
                    }
                }
            } else if (dateFilterApplied) {
                // keyword가 없을 경우 date 기반으로만 필터링
                scheduleAnswer = tableQueryService.filterAcademicScheduleByConditions(
                        null, startDate, endDate, dateFilterApplied, dataList);
                if (scheduleAnswer.isBlank()) {
                    scheduleAnswer = "요청하신 기간에는 학사일정이 없습니다.";
                }
            } else {
                // keyword도 없고 날짜도 없을 때
                scheduleAnswer = "어떤 학사일정을 찾으시나요? 예: 수강신청, 휴학 등 키워드를 입력해 주세요.";
            }

            response = new GptResponseDto(intent, scheduleAnswer);
        } else {
            String fallback = gptService.generateFallbackAnswer(userInput);
            response = new GptResponseDto("없음", fallback);
        }

        chatSessionService.saveSession(userId, intent, startDate.toString(), keyword, mealTime);
        return ResponseEntity.ok(response);
    }

    private boolean containsDateKeyword(String userInput) {
        return userInput.contains("오늘") ||
                userInput.contains("어제") ||
                userInput.contains("이번주") ||
                userInput.contains("이번 달") ||
                userInput.contains("이번달") ||
                userInput.contains("이번 주") ||
                userInput.contains("내일") ||
                userInput.contains("모레");
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) return null;
        if (keyword.contains("졸업")) return "학위수여";
        if (keyword.contains("학위수여")) return "졸업";
        return keyword;
    }
}