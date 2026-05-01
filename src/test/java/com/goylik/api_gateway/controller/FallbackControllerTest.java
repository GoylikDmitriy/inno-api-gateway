package com.goylik.api_gateway.controller;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(FallbackController.class)
class FallbackControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @ParameterizedTest
    @CsvSource({
            "/fallback/user, User service is temporarily unavailable. Please try again later.",
            "/fallback/auth, Auth service is temporarily unavailable. Please try again later.",
            "/fallback/order, Order service is temporarily unavailable. Please try again later.",
            "/fallback/payment, Payment service is temporarily unavailable. Please try again later."
    })
    void fallback_ShouldReturn503WithCorrectMessage(String path, String expectedMessage) {
        webTestClient.get().uri(path)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value())
                .jsonPath("$.error").isEqualTo("Service Unavailable")
                .jsonPath("$.details.message").isEqualTo(expectedMessage);
    }
}
