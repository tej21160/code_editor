package com.collabeditor.controller;

import com.collabeditor.entity.User;
import com.collabeditor.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        User user = userService.registerUser(username, password);
        Map<String, Object> response = Map.of("id", user.getId(), "username", user.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        Optional<User> userOpt = userService.loginUser(username, password);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Map<String, Object> response = Map.of("success", true, "userId", user.getId(), "username", user.getUsername());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(Map.of("success", false));
        }
    }
}