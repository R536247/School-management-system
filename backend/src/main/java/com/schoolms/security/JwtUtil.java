package com.schoolms.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key key;
    private final long accessExpiryMillis;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.access-expiry-minutes}") long accessExpiryMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessExpiryMillis = accessExpiryMinutes * 60 * 1000;
    }

    public String generateAccessToken(Long userId, Long schoolId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("school_id", schoolId)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + accessExpiryMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> validateToken(String token) throws JwtException {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }
}
