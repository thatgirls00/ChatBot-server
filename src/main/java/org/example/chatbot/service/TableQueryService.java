package org.example.chatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chatbot.domain.*;
import org.example.chatbot.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

import static org.example.chatbot.util.DateTimeExtractor.extractScheduleDateRange;

@Slf4j
@Service
@RequiredArgsConstructor
public class TableQueryService {

    private final StudentMealRepository studentMealRepository;
    private final FacultyMealRepository facultyMealRepository;
    private final DormMealRepository dormMealRepository;

    private final AcademicNoticeRepository academicNoticeRepository;
    private final AcademicScheduleRepository academicScheduleRepository;
    private final HankyongNoticeRepository hankyongNoticeRepository;
    private final ScholarshipNoticeRepository scholarshipNoticeRepository;

    public List<?> findMealDataByIntent(String intent, String keyword) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        return switch (intent) {
            case "학생식당" -> studentMealRepository.findAll();
            case "교직원식당" -> hasKeyword
                    ? facultyMealRepository.findByMenuContaining(keyword)
                    : facultyMealRepository.findAll();
            case "기숙사식당" -> dormMealRepository.findAll();
            default -> List.of();
        };
    }

    public List<?> findNoticeDataByIntent(String intent, String keyword) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        return switch (intent) {
            case "학사공지" -> hasKeyword
                    ? academicNoticeRepository.findByTitleContaining(keyword)
                    : academicNoticeRepository.findAll();
            case "장학공지" -> hasKeyword
                    ? scholarshipNoticeRepository.findByTitleContaining(keyword)
                    : scholarshipNoticeRepository.findAll();
            case "한경공지" -> hasKeyword
                    ? hankyongNoticeRepository.findByTitleContaining(keyword)
                    : hankyongNoticeRepository.findAll();
            case "학사일정" -> academicScheduleRepository.findAll();
            default -> List.of();
        };
    }

    public String filterMealByConditions(String intent, String keyword, String mealTime,
                                         LocalDate startDate, LocalDate endDate,
                                         boolean dateFilterApplied, List<?> dataList) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean keywordFound = false;
        Set<String> matchedMenus = new LinkedHashSet<>();
        boolean foundDateInRange = false;

        log.info("🔎 filterMealByConditions: mealTime={}, startDate={}, endDate={}", mealTime, startDate, endDate);

        for (Object data : dataList) {
            String menu = null, dateStr = null, timeSlot = null;

            if (data instanceof DormMeal meal) {
                menu = meal.getFormattedMenu() != null ? meal.getFormattedMenu() : meal.getMenu();
                dateStr = meal.getMealDate();
                timeSlot = "";
            } else if (data instanceof StudentMeal meal) {
                menu = meal.getMenu();
                dateStr = meal.getMealDate();
                timeSlot = meal.getMealTime();
            } else if (data instanceof FacultyMeal meal) {
                menu = meal.getMenu();
                dateStr = meal.getMealDate();
                timeSlot = meal.getMealTime();
            } else continue;

            if (dateStr == null || dateStr.isBlank()) continue;
            LocalDate mealDate = LocalDate.parse(dateStr);

            if (!mealDate.isBefore(startDate) && !mealDate.isAfter(endDate)) {
                foundDateInRange = true;
                if (menu == null || menu.isBlank() || "등록된 식단내용이(가) 없습니다.".equals(menu.trim())) continue;

                String extractedMenu = menu;

                if (mealTime != null && !mealTime.isBlank()) {
                    extractedMenu = extractMealSection(menu, mealTime);
                    if (extractedMenu == null) continue;
                } else if (mealTime != null && !mealTime.equalsIgnoreCase(timeSlot)) {
                    continue;
                }

                if (hasKeyword && extractedMenu.contains(keyword)) {
                    keywordFound = true;
                }

                matchedMenus.add(String.format("[%s]\n%s", mealDate, extractedMenu));
            }
        }

        if (!matchedMenus.isEmpty()) {
            if (hasKeyword) {
                if (keywordFound) {
                    return String.format("네, '%s' 메뉴가 포함되어 있어요.\n\n%s", keyword, String.join("\n\n", matchedMenus));
                } else {
                    return String.format("아니요, '%s' 메뉴는 없습니다.\n\n%s", keyword, String.join("\n\n", matchedMenus));
                }
            }
            return String.join("\n\n", matchedMenus);
        }

        if (dateFilterApplied) {
            if (!foundDateInRange) {
                return String.format("요청하신 기간(%s ~ %s)에는 식단 정보가 없어요. 다른 기간으로 다시 질문해 보시겠어요?", startDate, endDate);
            }
            if (hasKeyword) {
                return String.format("요청하신 기간(%s ~ %s)에는 식단은 있지만, '%s' 메뉴는 포함되어 있지 않아요.", startDate, endDate, keyword);
            }
            return String.format("요청하신 기간(%s ~ %s)에는 조건에 맞는 식단을 찾지 못했어요.", startDate, endDate);
        }

        return "최근 관련 식단을 찾지 못했어요.";
    }

    private String extractMealSection(String menu, String mealTime) {
        String marker = "[" + mealTime + "]";
        int start = menu.indexOf(marker);
        if (start == -1) return null;
        int nextMarker = menu.indexOf("[", start + marker.length());
        if (nextMarker == -1) nextMarker = menu.length();
        return menu.substring(start, nextMarker).trim();
    }

    public String filterNoticeByConditions(String keyword, LocalDate startDate, LocalDate endDate,
                                           boolean dateFilterApplied, List<?> dataList) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        Set<String> matchedNotices = new LinkedHashSet<>();
        Set<String> fallbackNotices = new LinkedHashSet<>();
        boolean foundDateInRange = false;
        int currentYear = LocalDate.now().getYear();

        for (Object data : dataList) {
            String title = null, dateStr = null;

            if (data instanceof AcademicNotice notice) {
                title = notice.getTitle();
                dateStr = notice.getNoticeDate();
            } else if (data instanceof ScholarshipNotice notice) {
                title = notice.getTitle();
                dateStr = notice.getNoticeDate();
            } else if (data instanceof HankyongNotice notice) {
                title = notice.getTitle();
                dateStr = notice.getNoticeDate();
            } else continue;

            if (dateStr == null || dateStr.isBlank()) continue;
            LocalDate noticeDate = LocalDate.parse(dateStr);

            if (!noticeDate.isBefore(startDate) && !noticeDate.isAfter(endDate)) {
                foundDateInRange = true;
                if (!hasKeyword || (title != null && title.contains(keyword))) {
                    matchedNotices.add(String.format("[%s] %s", noticeDate, title));
                }
            } else {
                boolean isThisYear = noticeDate.getYear() == currentYear;
                if (hasKeyword && isThisYear && title != null && title.contains(keyword)) {
                    fallbackNotices.add(String.format("[다른 날짜 %s] %s", dateStr, title));
                }
            }
        }

        if (!matchedNotices.isEmpty()) return String.join("\n\n", matchedNotices);

        if (dateFilterApplied && !foundDateInRange && !fallbackNotices.isEmpty()) {
            return String.format(
                    "요청하신 기간(%s ~ %s)에는 '%s' 키워드를 포함한 공지사항이 없어요.\n다른 날짜에 찾은 관련 공지사항은 다음과 같아요:\n\n%s",
                    startDate, endDate, keyword, String.join("\n\n", fallbackNotices)
            );
        }

        if (dateFilterApplied) {
            return String.format("요청하신 기간(%s ~ %s)에는 '%s' 키워드를 포함한 공지사항이 없어요. 다른 기간으로 다시 질문해 보시겠어요?", startDate, endDate, keyword);
        }

        if (!fallbackNotices.isEmpty()) {
            return "최근 관련 공지사항은 다음과 같아요:\n\n" + String.join("\n\n", fallbackNotices);
        }

        return "최근 관련 공지사항을 찾지 못했어요.";
    }

    public String filterAcademicScheduleByConditions(String keyword, LocalDate startDate, LocalDate endDate,
                                                     boolean dateFilterApplied, List<?> dataList) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        Set<String> matchedSchedules = new LinkedHashSet<>();
        boolean foundDateInRange = false;

        for (Object data : dataList) {
            if (!(data instanceof AcademicSchedule schedule)) continue;
            String content = schedule.getContent();
            if (content == null || content.isBlank()) continue;

            LocalDate[] scheduleRange = extractScheduleDateRange(content, startDate.getYear());
            if (scheduleRange == null) continue;

            LocalDate scheduleStart = scheduleRange[0], scheduleEnd = scheduleRange[1];
            boolean isOverlap = !scheduleStart.isAfter(endDate) && !scheduleEnd.isBefore(startDate);

            if (isOverlap) {
                foundDateInRange = true;
                if (dateFilterApplied && hasKeyword && !content.contains(keyword)) continue;
                matchedSchedules.add(String.format("[%s ~ %s] %s", scheduleStart, scheduleEnd, content));
            }
        }

        if (!matchedSchedules.isEmpty()) return String.join("\n\n", matchedSchedules);

        if (dateFilterApplied && hasKeyword) {
            String otherDate = findKeywordInOtherDates(keyword, startDate, endDate);
            if (!otherDate.isBlank()) {
                return String.format(
                        "요청하신 기간에는 '%s' 키워드 일정이 없지만, %s에 같은 일정이 있습니다.", keyword, otherDate);
            }
        }

        if (dateFilterApplied) {
            if (!foundDateInRange) {
                return String.format("요청하신 기간(%s ~ %s)에는 학사일정이 없어요. 다른 기간으로 다시 질문해 보시겠어요?", startDate, endDate);
            }
            if (hasKeyword) {
                return String.format("요청하신 기간에 학사일정은 있지만, '%s' 키워드를 포함한 내용은 보이지 않아요.", keyword);
            }
            return String.format("요청하신 기간(%s ~ %s)에는 조건에 맞는 학사일정을 찾지 못했어요.", startDate, endDate);
        }

        return "최근 관련 학사일정을 찾지 못했어요.";
    }

    public String findKeywordInOtherDates(String keyword, LocalDate startDate, LocalDate endDate) {
        List<AcademicSchedule> schedules = academicScheduleRepository.findByContentContaining(keyword);

        for (AcademicSchedule schedule : schedules) {
            String content = schedule.getContent();
            if (content == null || content.isBlank()) continue;

            LocalDate[] scheduleRange = extractScheduleDateRange(content, startDate.getYear());
            if (scheduleRange == null) continue;

            LocalDate scheduleStart = scheduleRange[0], scheduleEnd = scheduleRange[1];

            boolean isOutsideRequestedPeriod = scheduleEnd.isBefore(startDate) || scheduleStart.isAfter(endDate);
            if (isOutsideRequestedPeriod) {
                return String.format("%s ~ %s", scheduleStart, scheduleEnd);
            }
        }
        return "";
    }
}