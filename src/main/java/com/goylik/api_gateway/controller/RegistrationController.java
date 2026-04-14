package com.goylik.api_gateway.controller;

import com.goylik.api_gateway.model.dto.request.RegisterRequest;
import com.goylik.api_gateway.model.dto.response.UserResponse;
import com.goylik.api_gateway.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class RegistrationController {
    private final RegistrationService registrationService;

    /**
     * Public registration endpoint.
     *
     * Gateway handles this endpoint directly — NOT forwarded to user-service.
     * Orchestrates compensation transaction:
     *   1. Creates user in user-service
     *   2. Saves credentials in auth-service
     *   3. Rolls back user if auth-service fails
     *
     * @param request registration data (name, surname, email, password, birthDate)
     * @return created user info
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return registrationService.register(request);
    }
}
