package com.collabeditor;

import com.collabeditor.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Test
    public void testGenerateAndValidateToken() {
        String token = jwtService.generateToken("123", "testuser");
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    public void testExtractUserId() {
        String token = jwtService.generateToken("456", "testuser");
        assertEquals("456", jwtService.extractUserId(token));
    }

    @Test
    public void testExtractUsername() {
        String token = jwtService.generateToken("789", "alice");
        assertEquals("alice", jwtService.extractUsername(token));
    }

    @Test
    public void testInvalidToken() {
        assertFalse(jwtService.isTokenValid("invalid.token.here"));
    }
}
