package com.collabeditor.controller;

import com.collabeditor.model.CodeChangeMessage;
import com.collabeditor.service.DocumentStateService;
import com.collabeditor.service.OperationalTransformService;
import com.collabeditor.service.RateLimiterService;
import com.collabeditor.service.RedisRoomService;
import com.collabeditor.service.SnapshotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

@Controller
public class EditorController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private RedisRoomService redisRoomService;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private OperationalTransformService otService;

    @Autowired
    private DocumentStateService documentStateService;

    @Autowired
    private SnapshotService snapshotService;

    private ObjectMapper objectMapper = new ObjectMapper();
    
    private final java.util.concurrent.ConcurrentHashMap<String, AtomicInteger> editCounters = new java.util.concurrent.ConcurrentHashMap<>();

    @MessageMapping("/room/{roomId}/edit")
    public void handleCodeChange(@DestinationVariable String roomId,
                                  @Payload CodeChangeMessage message) {
        if (!rateLimiterService.isAllowed(message.getUserId())) {
            return; // silently drop the message
        }
        
        message.setRoomId(roomId);
        message.setTimestamp(LocalDateTime.now());
        
        // Set server version on message
        long serverVersion = documentStateService.incrementVersion(roomId);
        message.setVersion(serverVersion);
        
        // Save code snapshot to Redis
        redisRoomService.saveCodeSnapshot(roomId, message.getContent());
        
        // Every 30 edits, save persistent snapshot
        AtomicInteger counter = editCounters.computeIfAbsent(roomId, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();
        if (count % 30 == 0) {
            snapshotService.saveSnapshot(roomId, message.getContent(), serverVersion);
        }
        
        // Broadcast to all clients in room
        messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
    }

    @MessageMapping("/room/{roomId}/join")
    public void handleUserJoin(@DestinationVariable String roomId,
                                @Payload CodeChangeMessage message) {
        redisRoomService.addUserToRoom(roomId, message.getUserId(), message.getUsername());
        messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
    }
}
