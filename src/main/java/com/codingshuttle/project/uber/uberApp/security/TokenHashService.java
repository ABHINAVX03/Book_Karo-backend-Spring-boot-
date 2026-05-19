package com.codingshuttle.project.uber.uberApp.security;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class TokenHashService {

    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash token.", ex);
        }
    }
}
