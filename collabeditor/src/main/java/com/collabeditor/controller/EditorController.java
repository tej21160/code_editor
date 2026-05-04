package com.collabeditor.controller;

import com.collabeditor.model.CodeChangeMessage;
import com.collabeditor.service.RateLimiterService;
import com.collabeditor.service.RedisRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class EditorController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private RedisRoomService redisRoomService;

    @Autowired
    private RateLimiterService rateLimiterService;

    @MessageMapping("/room/{roomId}/edit")
    public void handleCodeChange(@DestinationVariable String roomId,
                                  @Payload CodeChangeMessage message) {
        if (!rateLimiterService.isAllowed(message.getUserId())) {
            return; // silently drop the message
        }
        message.setRoomId(roomId);
        message.setTimestamp(LocalDateTime.now());
        redisRoomService.saveCodeSnapshot(roomId, message.getContent());
        messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
    }

    @MessageMapping("/room/{roomId}/join")
    public void handleUserJoin(@DestinationVariable String roomId,
                                @Payload CodeChangeMessage message) {
        redisRoomService.addUserToRoom(roomId, message.getUserId(), message.getUsername());
        messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
    }
}
