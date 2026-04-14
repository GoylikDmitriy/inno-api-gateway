package com.goylik.api_gateway.controller.advice;

import com.goylik.api_gateway.exception.RegistrationException;
import com.goylik.api_gateway.model.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
@Slf4j
public class GatewayControllerAdvice {
    @ExceptionHandler(RegistrationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorResponse> handleRegistrationException(RegistrationException ex) {
        log.warn("Registration failed: {}", ex.getMessage());
        return Mono.just(ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Registration failed",
                ex.getMessage()
        ));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorResponse> handleValidationException(WebExchangeBindException ex) {
        String message = ex.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return Mono.just(ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message
        ));
    }

    @ExceptionHandler(WebClientResponseException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleWebClientResponseException(WebClientResponseException ex) {
        log.error("Downstream service error: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                "Downstream service error: " + ex.getMessage()
        );

        return Mono.just(ResponseEntity.status(status).body(errorResponse));
    }
}
