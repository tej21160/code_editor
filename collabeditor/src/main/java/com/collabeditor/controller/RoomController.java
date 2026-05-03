package com.collabeditor.controller;

import com.collabeditor.entity.Room;
import com.collabeditor.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @PostMapping("/create")
    public ResponseEntity<Room> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        UUID createdBy = UUID.fromString((String) body.get("createdBy"));
        Room room = roomService.createRoom(name, createdBy);
        return ResponseEntity.ok(room);
    }

    @PostMapping("/join")
    public ResponseEntity<Map<String, Object>> join(@RequestBody Map<String, Object> body) {
        UUID roomId = UUID.fromString((String) body.get("roomId"));
        UUID userId = UUID.fromString((String) body.get("userId"));
        var participant = roomService.joinRoom(roomId, userId);
        Map<String, Object> response = Map.of("success", true, "roomId", participant.getRoomId(), "userId", participant.getUserId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<Map<String, Object>> getDetails(@PathVariable UUID roomId) {
        Map<String, Object> details = roomService.getRoomDetails(roomId);
        if (details != null) {
            return ResponseEntity.ok(details);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}