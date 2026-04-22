package com.goylik.api_gateway.jwt;

import com.goylik.api_gateway.model.dto.response.TokenValidationResponse;
import com.goylik.api_gateway.model.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

@Service
public class JwtService {
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.clock-skew-seconds:30}")
    private long clockSkewSeconds;

    private static final String ACCESS_TOKEN_TYPE_STRING = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE_STRING = "REFRESH";

    public TokenValidationResponse extractAllFromAccessToken(String token) {
        return extractAll(token, ACCESS_TOKEN_TYPE_STRING);
    }

    public TokenValidationResponse extractAllFromRefreshToken(String token) {
        return extractAll(token, REFRESH_TOKEN_TYPE_STRING);
    }

    private TokenValidationResponse extractAll(String token, String type) {
        try {
            Claims claims = extractAllClaims(token);
            if (type.equals(claims.get("type", String.class))) {
                Long userId = Long.parseLong(claims.getSubject());
                Role role = Role.valueOf(claims.get("role", String.class));
                return TokenValidationResponse.success(userId, role);
            }

            return TokenValidationResponse.failure();
        } catch (Exception e) {
            return TokenValidationResponse.failure();
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .clockSkewSeconds(clockSkewSeconds)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }
}
