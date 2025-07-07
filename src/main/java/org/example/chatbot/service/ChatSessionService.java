package org.example.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final long TTL_MINUTES = 30;

    private static final String FIELD_INTENT = "lastIntent";
    private static final String FIELD_DATE = "lastDate";
    private static final String FIELD_KEYWORD = "lastKeyword";
    private static final String FIELD_MEAL_TIME = "lastMealTime";

    public void saveSession(String userId, String intent, String date, String keyword, String mealTime) {
        String key = buildKey(userId);
        redisTemplate.opsForHash().put(key, FIELD_INTENT, intent);
        redisTemplate.opsForHash().put(key, FIELD_DATE, date);
        redisTemplate.opsForHash().put(key, FIELD_KEYWORD, keyword);
        redisTemplate.opsForHash().put(key, FIELD_MEAL_TIME, mealTime);
        redisTemplate.expire(key, Duration.ofMinutes(TTL_MINUTES));
    }

    public String getLastIntent(String userId) {
        return getField(userId, FIELD_INTENT);
    }

    public String getLastDate(String userId) {
        return getField(userId, FIELD_DATE);
    }

    public String getLastKeyword(String userId) {
        return getField(userId, FIELD_KEYWORD);
    }

    public String getLastMealTime(String userId) {
        return getField(userId, FIELD_MEAL_TIME);
    }

    private String getField(String userId, String field) {
        Object value = redisTemplate.opsForHash().get(buildKey(userId), field);
        return value != null ? value.toString() : null;
    }

    private String buildKey(String userId) {
        return "chat:session:" + userId;
    }
}