package com.smartinstitute.erp.auth.service;

import com.smartinstitute.erp.auth.entity.RefreshToken;
import com.smartinstitute.erp.auth.repository.RefreshTokenRepository;
import com.smartinstitute.erp.exception.TokenException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import lombok.AllArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
@AllArgsConstructor
public class JwtService {
    private final RefreshTokenRepository refreshTokenRepository;
    private static final String ACCESS_SECRET="access-token-secret-key-256-bits-long-secret!!";
    private static final String REFRESH_SECRET="refresh-token-secret-key-256-bits-long-secret!!";

    private static final long ACCESS_EXP=1000*60*15;
    private static final long REFRESH_EXP=1000*60*60*24*7;

    private static final Key accessKey= Keys.hmacShaKeyFor(ACCESS_SECRET.getBytes());
    private static final Key refreshKey= Keys.hmacShaKeyFor(REFRESH_SECRET.getBytes());


    public String generateAccessToken(String email,String role,Long instituteId){
        Map<String,Object> claims=new HashMap<>();
        claims.put("type", "access");
        claims.put("role", role);
        if (instituteId!=null){
            claims.put("instituteId",instituteId);
        }
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+ACCESS_EXP))
                .signWith(accessKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String email,String jti){
        return Jwts.builder()
                .setSubject(email)
                .claim("jti",jti)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+REFRESH_EXP))
                .signWith(refreshKey,SignatureAlgorithm.HS256)
                .compact();
    }


    public static Cookie generateRefreshCookie(String refreshToken){
        Cookie cookie=new Cookie("refreshToken",refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/auth/refresh");
        cookie.setMaxAge((int) (REFRESH_EXP/1000));
        return cookie;
    }

    public String extractEmail(String token){
        return Jwts.parserBuilder()
                .setSigningKey(accessKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String extractRole(String token){
        return Jwts.parserBuilder()
                .setSigningKey(accessKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role",String.class);
    }

    /**
     * JWT JSON often stores numeric claims as Integer; {@code get(..., Long.class)} then returns null.
     * Normalize any {@link Number} (or numeric string) to {@link Long}.
     */
    public Long extractInstituteId(String token) {
        Object raw = Jwts.parserBuilder()
                .setSigningKey(accessKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("instituteId");
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number n) {
            return n.longValue();
        }
        if (raw instanceof String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }


    public String extractEmailFromRefresh(String token){
        return Jwts.parserBuilder()
                .setSigningKey(refreshKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    //====Validate===

    public boolean validateAccessToken(String token){
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            var body = Jwts.parserBuilder()
                    .setSigningKey(accessKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String type = body.get("type", String.class);
            // Accept token if type is "access" or missing (backward compatibility)
            return type == null || "access".equals(type);
        } catch (JwtException e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token){
        try {
            Jwts.parserBuilder()
                    .setSigningKey(refreshKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        }catch (JwtException e){
            return false;
        }
    }

    //======Rotation+Device====

    public RefreshToken validateRefreshWithDevice(
            String rawRefreshToken,
            String requestDeviceId,
            String requestUserAgent
    ){
        String tokenHash=hashToken(rawRefreshToken);
        RefreshToken refreshToken =refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(()->new TokenException("Refresh token not found"));

        if(refreshToken.isRevoked()){
            throw new TokenException("Refresh token revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())){
            throw new TokenException("Refresh token expired");
        }

        if (refreshToken.getReplacedBy()!=null){
            throw  new TokenException("Refresh token already rotated");
        }

        if (!Objects.equals(refreshToken.getDeviceId(),requestDeviceId)){
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new RuntimeException("Device mismatched,token revoked");
        }

        if (refreshToken.getUserAgentHash()!=null){
            String incomingUaHash= DigestUtils.sha256Hex(requestUserAgent);
            if (!Objects.equals(refreshToken.getUserAgentHash(), incomingUaHash)) {
                refreshToken.setRevoked(true);
                refreshTokenRepository.save(refreshToken);
                throw new RuntimeException("User-Agent mismatch");
            }
        }

        refreshToken.setLastUsedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);

        return refreshToken;
    }

    public String hashToken(String token) {
        return DigestUtils.sha256Hex(token);
    }



}

