package com.vn.backend.configs.jwt;


import com.vn.backend.services.CustomUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

//sinh ra jwt
@Slf4j
@Component
public class JwtTokenProvider {
    @Value("${spring.plugin.springsecurity.rest.token.storage.jwt.secret}")
    private String JWT_SECRET;
    @Value("${spring.plugin.springsecurity.rest.token.storage.jwt.expiration}")
    private int JWT_EXPIRATION;
    @Value("${spring.plugin.springsecurity.rest.token.storage.jwt.refresh.expiration}")
    private int JWT_REFRESH_EXPIRATION;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
    }

    public String generateToken(CustomUserDetails customUserDetails) {
        Date now = new Date();
        Date dateExpiration = new Date(now.getTime() + JWT_EXPIRATION * 1000L);
        return Jwts.builder()
                .setSubject(customUserDetails.getUsername())
                .claim("id", customUserDetails.getId())
                .setIssuedAt(now)
                .setExpiration(dateExpiration)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(CustomUserDetails customUserDetails) {
        Date now = new Date();
        Date dateExpiration = new Date(now.getTime() + JWT_REFRESH_EXPIRATION * 1000L);
        return Jwts.builder()
                .setSubject(customUserDetails.getUsername())
                .claim("id", customUserDetails.getId())
                .setIssuedAt(now)
                .setExpiration(dateExpiration)
                .claim("type", "refresh")
                .signWith(getSigningKey())
                .compact();
    }

    public String getEmailFromJwt(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token).getBody();

        return claims.getSubject();
    }

    public String getUsernameFromJwt(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token).getBody();

        return claims.getSubject();
    }

    public Long getUserIdFromJwt(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("id", Long.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT Token");
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT Token");
        } catch (UnsupportedJwtException e) {
            log.error("UnsupportedJwt");
        } catch (IllegalArgumentException e) {
            log.error("JWT Claims String is empty");
        }
        return false;
    }

    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Kiểm tra xem token có phải là refresh token không
            String tokenType = claims.get("type", String.class);
            return "refresh".equals(tokenType);
        } catch (MalformedJwtException e) {
            log.error("Invalid Refresh Token");
        } catch (ExpiredJwtException e) {
            log.error("Expired Refresh Token");
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported Refresh Token");
        } catch (IllegalArgumentException e) {
            log.error("Refresh Token Claims String is empty");
        }
        return false;
    }

}
