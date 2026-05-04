package com.collabeditor.service;

import com.collabeditor.entity.User;
import com.collabeditor.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    public User registerUser(String username, String password) {
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = User.builder().username(username).passwordHash(hash).createdAt(LocalDateTime.now()).build();
        return userRepository.save(user);
    }

    public Map<String, Object> loginUser(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent() && BCrypt.checkpw(password, userOpt.get().getPasswordHash())) {
            User user = userOpt.get();
            String token = jwtService.generateToken(user.getId().toString(), user.getUsername());
            return Map.of("userId", user.getId().toString(), "username", user.getUsername(), "success", true, "token", token);
        }
        return Map.of("success", false);
    }
}