package com.goylik.api_gateway.controller;

import com.goylik.api_gateway.model.dto.request.RegisterRequest;
import com.goylik.api_gateway.model.dto.response.UserResponse;
import com.goylik.api_gateway.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(RegistrationController.class)
class RegistrationControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private RegistrationService registrationService;

    @Test
    void register_returnsCreatedUser() {
        RegisterRequest request = new RegisterRequest("John", "Doe", LocalDate.of(2000, 1, 1), "john@test.com", "password");
        UserResponse response = new UserResponse(1L, "John", "Doe", LocalDate.of(2000, 1, 1), "john@test.com", true);

        when(registrationService.register(any(RegisterRequest.class)))
                .thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1L)
                .jsonPath("$.name").isEqualTo("John");
    }

    @Test
    void register_returnsBadRequest_whenInvalidData() {
        RegisterRequest invalidRequest = new RegisterRequest("", "", null, "not-an-email", "");

        webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
