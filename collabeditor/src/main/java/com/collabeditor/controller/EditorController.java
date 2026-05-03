package com.collabeditor.controller;

import com.collabeditor.model.CodeChangeMessage;
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

    @MessageMapping("/room/{roomId}/edit")
    public void handleCodeChange(@DestinationVariable String roomId,
                                  @Payload CodeChangeMessage message) {
        message.setRoomId(roomId);
        message.setTimestamp(LocalDateTime.now());
        messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
    }

    @MessageMapping("/room/{roomId}/join")
    public void handleUserJoin(@DestinationVariable String roomId,
                                @Payload CodeChangeMessage message) {
        messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
    }
}
