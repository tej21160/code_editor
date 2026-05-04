package com.collabeditor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service  
public class DocumentStateService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String VERSION_PREFIX = "doc:version:";
    private static final String OPS_PREFIX = "doc:ops:";

    public long getCurrentVersion(String roomId) {
        String key = VERSION_PREFIX + roomId;
        String version = (String) redisTemplate.opsForValue().get(key);
        return version != null ? Long.parseLong(version) : 0L;
    }

    public long incrementVersion(String roomId) {
        String key = VERSION_PREFIX + roomId;
        Long newVersion = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 24, TimeUnit.HOURS);
        return newVersion != null ? newVersion : 1L;
    }

    public void saveOperation(String roomId, String operationJson) {
        String key = OPS_PREFIX + roomId;
        redisTemplate.opsForList().rightPush(key, operationJson);
        redisTemplate.expire(key, 24, TimeUnit.HOURS);
        // Keep only last 100 operations
        redisTemplate.opsForList().trim(key, -100, -1);
    }

    public List<String> getRecentOperations(String roomId) {
        String key = OPS_PREFIX + roomId;
        List<String> ops = redisTemplate.opsForList().range(key, 0, -1);
        if (ops == null) return new ArrayList<>();
        return ops;
    }
}
