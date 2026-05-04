package com.collabeditor.controller;

import com.collabeditor.entity.CodeSnapshot;
import com.collabeditor.service.RedisRoomService;
import com.collabeditor.service.SnapshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/room-state")
public class RoomStateController {
    
    @Autowired
    private RedisRoomService redisRoomService;
    
    @Autowired
    private SnapshotService snapshotService;
    
    // GET /api/room-state/{roomId}/users
    // Returns all currently connected users in the room
    @GetMapping("/{roomId}/users")
    public ResponseEntity<Map<Object, Object>> getRoomUsers(@PathVariable String roomId) {
        Map<Object, Object> users = redisRoomService.getRoomUsers(roomId);
        return ResponseEntity.ok(users);
    }
    
    // GET /api/room-state/{roomId}/code
    // Returns the latest code snapshot for reconnecting users
    @GetMapping("/{roomId}/code")
    public ResponseEntity<Map<String, String>> getRoomCode(@PathVariable String roomId) {
        String code = redisRoomService.getCodeSnapshot(roomId);
        Map<String, String> response = new HashMap<>();
        response.put("code", code != null ? code : "");
        return ResponseEntity.ok(response);
    }
    
    // GET /api/room-state/{roomId}/history
    // Returns the snapshot history for the room
    @GetMapping("/{roomId}/history")
    public ResponseEntity<List<CodeSnapshot>> getRoomHistory(@PathVariable String roomId) {
        List<CodeSnapshot> history = snapshotService.getSnapshotHistory(roomId);
        return ResponseEntity.ok(history);
    }
}
