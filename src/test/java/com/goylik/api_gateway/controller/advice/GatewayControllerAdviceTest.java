package com.goylik.api_gateway.controller.advice;

import com.goylik.api_gateway.exception.RegistrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayControllerAdviceTest {
    private GatewayControllerAdvice advice;

    @BeforeEach
    void setUp() {
        advice = new GatewayControllerAdvice();
    }

    @Test
    void handleRegistrationException_ShouldReturn400() {
        var ex = new RegistrationException("Email already exists");

        StepVerifier.create(advice.handleRegistrationException(ex))
                .expectNextMatches(response ->
                        response.status() == 400 &&
                                response.error().equals("Registration failed") &&
                                response.details().get("message").equals("Email already exists"))
                .verifyComplete();
    }

    @Test
    void handleValidationException_ShouldReturn400_WithFieldError() {
        var bindingResult = mock(BindingResult.class);
        var fieldError = new FieldError("registerRequest", "email", "must be a valid email");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        var ex = new WebExchangeBindException(
                mock(MethodParameter.class), bindingResult);

        StepVerifier.create(advice.handleValidationException(ex))
                .expectNextMatches(response ->
                        response.status() == 400 &&
                                response.details().get("message").contains("email"))
                .verifyComplete();
    }

    @Test
    void handleWebClientResponseException_ShouldReturnSameStatus() {
        var ex = new WebClientResponseException(
                409, "Conflict", null, null, null);

        StepVerifier.create(advice.handleWebClientResponseException(ex))
                .expectNextMatches(response ->
                        response.getStatusCode().value() == 409)
                .verifyComplete();
    }

    @Test
    void handleWebClientResponseException_ShouldReturn503_WhenServiceUnavailable() {
        var ex = new WebClientResponseException(
                503, "Service Unavailable", null, null, null);

        StepVerifier.create(advice.handleWebClientResponseException(ex))
                .expectNextMatches(response ->
                        response.getStatusCode().value() == 503)
                .verifyComplete();
    }

    @Test
    void handleWebClientResponseException_ShouldReturn404_WhenNotFound() {
        var ex = new WebClientResponseException(
                404, "Not Found", null, null, null);

        StepVerifier.create(advice.handleWebClientResponseException(ex))
                .expectNextMatches(response ->
                        response.getStatusCode().value() == 404)
                .verifyComplete();
    }
}