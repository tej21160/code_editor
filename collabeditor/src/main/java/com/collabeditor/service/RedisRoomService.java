package com.collabeditor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class RedisRoomService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private static final String ROOM_USERS_PREFIX = "room:users:";
    private static final String ROOM_CODE_PREFIX = "room:code:";
    private static final int ROOM_EXPIRY_HOURS = 24;
    
    // Add user to room
    public void addUserToRoom(String roomId, String userId, String username) {
        String key = ROOM_USERS_PREFIX + roomId;
        redisTemplate.opsForHash().put(key, userId, username);
        redisTemplate.expire(key, ROOM_EXPIRY_HOURS, TimeUnit.HOURS);
    }
    
    // Remove user from room
    public void removeUserFromRoom(String roomId, String userId) {
        String key = ROOM_USERS_PREFIX + roomId;
        redisTemplate.opsForHash().delete(key, userId);
    }
    
    // Get all users in room
    public Map<Object, Object> getRoomUsers(String roomId) {
        String key = ROOM_USERS_PREFIX + roomId;
        return redisTemplate.opsForHash().entries(key);
    }
    
    // Save latest code snapshot
    public void saveCodeSnapshot(String roomId, String code) {
        String key = ROOM_CODE_PREFIX + roomId;
        redisTemplate.opsForValue().set(key, code, ROOM_EXPIRY_HOURS, TimeUnit.HOURS);
    }
    
    // Get latest code snapshot
    public String getCodeSnapshot(String roomId) {
        String key = ROOM_CODE_PREFIX + roomId;
        return (String) redisTemplate.opsForValue().get(key);
    }
    
    // Get room user count
    public long getRoomUserCount(String roomId) {
        String key = ROOM_USERS_PREFIX + roomId;
        return redisTemplate.opsForHash().size(key);
    }
}
