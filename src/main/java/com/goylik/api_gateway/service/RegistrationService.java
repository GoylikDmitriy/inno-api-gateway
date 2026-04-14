package com.goylik.api_gateway.service;

import com.goylik.api_gateway.exception.RegistrationException;
import com.goylik.api_gateway.model.dto.request.CreateUserRequest;
import com.goylik.api_gateway.model.dto.request.RegisterRequest;
import com.goylik.api_gateway.model.dto.request.SaveCredentialsRequest;
import com.goylik.api_gateway.model.dto.response.UserResponse;
import com.goylik.api_gateway.model.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {
    private final WebClient webClient;

    @Value("${app.urls.user-service}")
    private String userServiceUrl;

    @Value("${app.urls.auth-service}")
    private String authServiceUrl;

    @Value("${app.internal.api-keys.gateway}")
    private String internalApiKey;

    public Mono<UserResponse> register(RegisterRequest request) {
        return createUser(request)
                .flatMap(createdUser ->
                        saveCredentials(request, createdUser.id())
                                .thenReturn(createdUser)
                                .onErrorResume(ex -> rollback(createdUser.id(), ex))
                );
    }

    private Mono<UserResponse> createUser(RegisterRequest request) {
        return webClient.post()
                .uri(userServiceUrl + "/api/users/register")
                .bodyValue(new CreateUserRequest(
                        request.name(),
                        request.surname(),
                        request.birthDate(),
                        request.email()
                ))
                .retrieve()
                .bodyToMono(UserResponse.class)
                .doOnSuccess(u -> log.info("Step 1 success: user created with id={}", u.id()))
                .onErrorMap(WebClientResponseException.class, ex -> {
                    log.error("Step 1 failed: could not create user. Status={}", ex.getStatusCode());

                    if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                        return new RegistrationException(
                                "Email already exists: " + request.email());
                    }

                    return new RegistrationException(
                            "Failed to create user: " + ex.getMessage());
                });
    }

    private Mono<Void> saveCredentials(RegisterRequest request, Long userId) {
        return webClient.post()
                .uri(authServiceUrl + "/api/auth/save-credentials")
                .header("X-Internal-Api-Key", internalApiKey)
                .bodyValue(new SaveCredentialsRequest(
                        userId,
                        request.email(),
                        request.password(),
                        Role.ROLE_USER
                ))
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("Step 2 success: credentials saved for userId={}", userId))
                .onErrorMap(WebClientResponseException.class, ex -> {
                    log.error("Step 2 failed: could not save credentials. Status={}", ex.getStatusCode());

                    if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                        return new RegistrationException(
                                "Credentials are already exist in auth service: " + request.email());
                    }

                    return new RegistrationException(
                            "Failed to save credentials: " + ex.getMessage());
                });
    }

    private Mono<UserResponse> rollback(Long userId, Throwable cause) {
        return deleteUser(userId)
                .doOnSuccess(v -> log.info("Compensation success: user {} deleted", userId))
                .then(Mono.error(cause))
                .cast(UserResponse.class);
    }

    private Mono<Void> deleteUser(Long userId) {
        return webClient.delete()
                .uri(userServiceUrl + "/api/users/internal/{id}", userId)
                .header("X-Internal-Api-Key", internalApiKey)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(ex -> {
                    log.error("CRITICAL: Compensation failed for userId={}. " +
                            "User exists in user-service without credentials. " +
                            "Manual cleanup required. Error: {}", userId, ex.getMessage());
                    return Mono.empty();
                });
    }
}
