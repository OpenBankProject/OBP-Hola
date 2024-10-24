package com.openbankproject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void save(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    // Save a value with a TTL (Time-to-Live)
    public void saveWithTTL(String key, String value, long timeoutInSeconds) {
        save(key, value);
        // Set TTL for the key
        redisTemplate.expire(key, timeoutInSeconds, TimeUnit.SECONDS);
    }
    
    // Save a value with a TTL (Time-to-Live)
    public void appendWithTTL(String key, String value, long timeoutInSeconds) {
        String currentValue = get(key);
        String currentValuePrintFriendly = currentValue != null ? currentValue : "";
        save(key, currentValuePrintFriendly + value);
        // Set TTL for the key
        redisTemplate.expire(key, timeoutInSeconds, TimeUnit.SECONDS);
    }


    public String get(String key) {
        return (String) redisTemplate.opsForValue().get(key);
    }
}
