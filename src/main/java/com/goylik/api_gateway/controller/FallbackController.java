package com.goylik.api_gateway.controller;

import com.goylik.api_gateway.model.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fallback")
public class FallbackController {
    @RequestMapping("/user")
    public Mono<ResponseEntity<ErrorResponse>> userFallback() {
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                "User service is temporarily unavailable. Please try again later."
        );

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse));
    }

    @RequestMapping("/auth")
    public Mono<ResponseEntity<ErrorResponse>> authFallback() {
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                "Auth service is temporarily unavailable. Please try again later."
        );

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse));
    }

    @RequestMapping("/order")
    public Mono<ResponseEntity<ErrorResponse>> orderFallback() {
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                "Order service is temporarily unavailable. Please try again later."
        );

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse));
    }

    @RequestMapping("/payment")
    public Mono<ResponseEntity<ErrorResponse>> paymentFallback() {
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                "Payment service is temporarily unavailable. Please try again later."
        );

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse));
    }
}
