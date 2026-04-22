package com.goylik.api_gateway.jwt;

import com.goylik.api_gateway.model.dto.response.TokenValidationResponse;
import com.goylik.api_gateway.model.enums.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {
    private JwtService jwtService;

    private static final String TEST_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "clockSkewSeconds", 0);
    }

    @Test
    void extractAllFromAccessToken_ShouldReturnSuccess_WhenValidToken() {
        String token = buildToken(1L, Role.ROLE_USER, "ACCESS", 900000);

        TokenValidationResponse result = jwtService.extractAllFromAccessToken(token);

        assertTrue(result.valid());
        assertEquals(1L, result.userId());
        assertEquals(Role.ROLE_USER, result.role());
    }

    @Test
    void extractAllFromAccessToken_ShouldReturnFailure_WhenTokenIsExpired() {
        String token = buildToken(1L, Role.ROLE_USER, "ACCESS", -1000);

        TokenValidationResponse result = jwtService.extractAllFromAccessToken(token);

        assertFalse(result.valid());
        assertNull(result.userId());
        assertNull(result.role());
    }

    @Test
    void extractAllFromAccessToken_ShouldReturnFailure_WhenTokenIsRefreshType() {
        String token = buildToken(1L, Role.ROLE_USER, "REFRESH", 900000);

        TokenValidationResponse result = jwtService.extractAllFromAccessToken(token);

        assertFalse(result.valid());
    }

    @Test
    void extractAllFromAccessToken_ShouldReturnFailure_WhenTokenIsMalformed() {
        TokenValidationResponse result = jwtService.extractAllFromAccessToken("invalid.token.here");

        assertFalse(result.valid());
        assertNull(result.userId());
        assertNull(result.role());
    }

    @Test
    void extractAllFromAccessToken_ShouldReturnFailure_WhenTokenIsEmpty() {
        TokenValidationResponse result = jwtService.extractAllFromAccessToken("");

        assertFalse(result.valid());
    }

    @Test
    void extractAllFromRefreshToken_ShouldReturnSuccess_WhenValidToken() {
        String token = buildToken(1L, Role.ROLE_ADMIN, "REFRESH", 604800000);

        TokenValidationResponse result = jwtService.extractAllFromRefreshToken(token);

        assertTrue(result.valid());
        assertEquals(1L, result.userId());
        assertEquals(Role.ROLE_ADMIN, result.role());
    }

    @Test
    void extractAllFromRefreshToken_ShouldReturnFailure_WhenTokenIsAccessType() {
        String token = buildToken(1L, Role.ROLE_USER, "ACCESS", 900000);

        TokenValidationResponse result = jwtService.extractAllFromRefreshToken(token);

        assertFalse(result.valid());
    }

    @Test
    void extractAllFromRefreshToken_ShouldReturnFailure_WhenTokenIsExpired() {
        String token = buildToken(1L, Role.ROLE_USER, "REFRESH", -1000);

        TokenValidationResponse result = jwtService.extractAllFromRefreshToken(token);

        assertFalse(result.valid());
    }

    @Test
    void extractAllFromAccessToken_ShouldReturnAdminRole_WhenAdminToken() {
        String token = buildToken(99L, Role.ROLE_ADMIN, "ACCESS", 900000);

        TokenValidationResponse result = jwtService.extractAllFromAccessToken(token);

        assertTrue(result.valid());
        assertEquals(99L, result.userId());
        assertEquals(Role.ROLE_ADMIN, result.role());
    }

    private String buildToken(Long userId, Role role, String type, long expirationMs) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claims(Map.of(
                        "role", role.name(),
                        "type", type
                ))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET)))
                .compact();
    }
}
