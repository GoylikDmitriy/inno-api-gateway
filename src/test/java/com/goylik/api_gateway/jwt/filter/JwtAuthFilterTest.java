package com.goylik.api_gateway.jwt.filter;

import com.goylik.api_gateway.config.ApiPathsProperties;
import com.goylik.api_gateway.jwt.JwtService;
import com.goylik.api_gateway.model.dto.response.TokenValidationResponse;
import com.goylik.api_gateway.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {
    @Mock private JwtService jwtService;
    @Mock private ApiPathsProperties apiPathsProperties;
    @Mock private GatewayFilterChain chain;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtService, apiPathsProperties);
    }

    @Test
    void shouldSkipPublicPath() {
        when(apiPathsProperties.getPublicPaths()).thenReturn(List.of("/api/auth/login", "/api/auth/register"));
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/login").build()
        );
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldReturnUnauthorizedWhenNoAuthHeader() {
        when(apiPathsProperties.getPublicPaths()).thenReturn(List.of());
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders").build()
        );

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(exchange);
    }

    @Test
    void shouldReturnUnauthorizedWhenInvalidToken() {
        when(apiPathsProperties.getPublicPaths()).thenReturn(List.of());
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .build()
        );
        when(jwtService.extractAllFromAccessToken("invalid-token"))
                .thenReturn(TokenValidationResponse.failure());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(exchange);
    }

    @Test
    void shouldAuthenticateAndForwardHeaders() {
        when(apiPathsProperties.getPublicPaths()).thenReturn(List.of());
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .build()
        );
        TokenValidationResponse validation = TokenValidationResponse.success(123L, Role.ROLE_USER);
        when(jwtService.extractAllFromAccessToken("valid-token")).thenReturn(validation);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        ServerHttpRequest modifiedRequest = captor.getValue().getRequest();
        assertThat(modifiedRequest.getHeaders().getFirst("X-User-Id")).isEqualTo("123");
        assertThat(modifiedRequest.getHeaders().getFirst("X-User-Role")).isEqualTo("ROLE_USER");
    }
}
