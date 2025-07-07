package org.example.chatbot.util;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateTimeExtractor {

    /**
     * 사용자 입력에서 날짜 범위를 추출합니다. (기본: 오늘~오늘)
     * - "오늘/내일/모레/어제"는 단일 날짜
     * - "이번주"는 이번주 월요일~일요일 범위
     * - "이번달"은 이번 달 1일~말일까지 범위
     * - "6월 15일", "MM-DD", "yyyy-MM-dd" 형태도 현재 연도로 파싱
     */
    public static LocalDate[] extractDateRange(String userInput) {
        int currentYear = LocalDate.now(Clock.systemUTC()).getYear();
        LocalDate today = LocalDate.now(Clock.systemUTC());

        if (userInput.contains("오늘")) {
            return new LocalDate[]{today, today};
        } else if (userInput.contains("내일")) {
            LocalDate date = today.plusDays(1);
            return new LocalDate[]{date, date};
        } else if (userInput.contains("모레")) {
            LocalDate date = today.plusDays(2);
            return new LocalDate[]{date, date};
        } else if (userInput.contains("어제")) {
            LocalDate date = today.minusDays(1);
            return new LocalDate[]{date, date};
        } else if (userInput.contains("이번주") || userInput.contains("이번 주")) {
            LocalDate monday = today.with(DayOfWeek.MONDAY);
            LocalDate sunday = monday.plusDays(6);
            return new LocalDate[]{monday, sunday};
        } else if (userInput.contains("이번달") || userInput.contains("이번 달")) {
            LocalDate startOfMonth = today.withDayOfMonth(1);
            LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
            return new LocalDate[]{startOfMonth, endOfMonth};
        }

        // MM월 DD일 형태 파싱
        Pattern mdPattern = Pattern.compile("(\\d{1,2})월\\s*(\\d{1,2})일");
        Matcher mdMatcher = mdPattern.matcher(userInput);
        if (mdMatcher.find()) {
            try {
                int month = Integer.parseInt(mdMatcher.group(1));
                int day = Integer.parseInt(mdMatcher.group(2));
                LocalDate date = LocalDate.of(currentYear, month, day);
                System.out.println("[extractDateRange] MM월 DD일 파싱 성공: " + date);
                return new LocalDate[]{date, date};
            } catch (Exception e) {
                System.out.println("[extractDateRange] MM월 DD일 파싱 실패, fallback");
                return new LocalDate[]{today, today};
            }
        }

        // yyyy-MM-dd 또는 MM-DD 형태 파싱
        userInput = userInput.replaceAll("\\.", "-");
        String cleaned = userInput.replaceAll("[^0-9\\-]", "");

        try {
            if (cleaned.matches("\\d{2}-\\d{2}")) {
                cleaned = currentYear + "-" + cleaned;
            }
            LocalDate date = LocalDate.parse(cleaned, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            System.out.println("[extractDateRange] yyyy-MM-dd/MM-DD 파싱 성공: " + date);
            return new LocalDate[]{date, date};
        } catch (Exception e) {
            System.out.println("[extractDateRange] yyyy-MM-dd/MM-DD 파싱 실패, fallback");
            return new LocalDate[]{today, today};
        }
    }

    /**
     * 사용자 입력에서 식사 시간대 추출.
     * - 학생식당 특수: 맛난한끼/건강한끼 키워드 지원
     * - 교직원/기숙사 식당: 아침/점심/저녁 키워드
     * - 없으면 null 반환
     */
    public static String extractMealTime(String userInput) {
        // 학생식당 특수 처리
        if (userInput.contains("맛난")) return "맛난한끼";
        if (userInput.contains("건강")) return "건강한끼";

        // 일반 시간대
        if (userInput.contains("아침")) return "아침";
        if (userInput.contains("점심")) return "점심";
        if (userInput.contains("저녁")) return "저녁";

        return null;
    }

    /**
     * 학사일정 content에서 MM.DD (요일) ~ MM.DD (요일) 형태의 기간을 추출해 LocalDate 배열로 반환합니다.
     * - 연도는 현재 연도로 가정 (UTC 기준)
     * - 종료일이 시작일보다 앞이면 종료일 연도를 +1년으로 보정
     * - 단일 날짜 형태(MM.DD (요일))도 지원
     */
    public static LocalDate[] extractScheduleDateRange(String content, int year) {
        Pattern pattern = Pattern.compile("(\\d{2}\\.\\d{2})\\s*\\([^)]+\\)\\s*~\\s*(\\d{2}[\\.\\-]\\d{2})");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            try {
                String startStr = matcher.group(1).replace(".", "-");
                String endStr = matcher.group(2).replace(".", "-");

                LocalDate start = LocalDate.parse(year + "-" + startStr);
                LocalDate end = LocalDate.parse(year + "-" + endStr);

                if (end.isBefore(start)) {
                    end = end.plusYears(1);
                }

                return new LocalDate[]{start, end};
            } catch (DateTimeParseException e) {
                return null;
            }
        }

        // 단일 날짜 형태 MM.DD (요일) 처리
        pattern = Pattern.compile("(\\d{2}\\.\\d{2})\\s*\\([^)]+\\)");
        matcher = pattern.matcher(content);
        if (matcher.find()) {
            try {
                LocalDate date = LocalDate.parse(year + "-" + matcher.group(1).replace(".", "-"));
                return new LocalDate[]{date, date};
            } catch (DateTimeParseException e) {
                return null;
            }
        }

        return null;
    }
}