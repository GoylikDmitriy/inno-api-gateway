package com.goylik.api_gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.goylik.api_gateway.exception.RegistrationException;
import com.goylik.api_gateway.model.dto.request.RegisterRequest;
import com.goylik.api_gateway.model.dto.response.UserResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationServiceTest {
    private MockWebServer mockWebServer;
    private RegistrationService registrationService;
    private ObjectMapper objectMapper;

    private static final String INTERNAL_API_KEY = "test-internal-key";

    private RegisterRequest validRequest;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        String baseUrl = mockWebServer.url("/").toString();
        WebClient webClient = WebClient.builder().build();

        registrationService = new RegistrationService(webClient);
        ReflectionTestUtils.setField(registrationService, "userServiceUrl", baseUrl);
        ReflectionTestUtils.setField(registrationService, "authServiceUrl", baseUrl);
        ReflectionTestUtils.setField(registrationService, "internalApiKey", INTERNAL_API_KEY);

        validRequest = new RegisterRequest(
                "John",
                "Doe",
                LocalDate.of(2000, 1, 1),
                "john@test.com",
                "password123"
        );

        userResponse = new UserResponse(
                1L,
                "John",
                "Doe",
                LocalDate.of(2000, 1, 1),
                "john@test.com",
                true
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void register_ShouldReturnUserResponse_WhenBothServicesSucceed() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(userResponse)));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201));

        StepVerifier.create(registrationService.register(validRequest))
                .expectNextMatches(response ->
                        response.id().equals(1L) &&
                                response.email().equals("john@test.com") &&
                                response.name().equals("John"))
                .verifyComplete();

        RecordedRequest createUserRequest = mockWebServer.takeRequest();
        assertThat(createUserRequest.getPath()).contains("/api/users/register");
        assertThat(createUserRequest.getMethod()).isEqualTo("POST");

        RecordedRequest saveCredentialsRequest = mockWebServer.takeRequest();
        assertThat(saveCredentialsRequest.getPath()).contains("/api/auth/save-credentials");
        assertThat(saveCredentialsRequest.getHeader("X-Internal-Api-Key"))
                .isEqualTo(INTERNAL_API_KEY);
    }

    @Test
    void register_ShouldThrowRegistrationException_WhenUserServiceReturns409() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(409)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"message\": \"Email already exists\"}"));

        StepVerifier.create(registrationService.register(validRequest))
                .expectErrorMatches(ex ->
                        ex instanceof RegistrationException &&
                                ex.getMessage().contains("Email already exists"))
                .verify();
    }

    @Test
    void register_ShouldThrowRegistrationException_WhenUserServiceReturns500() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500));

        StepVerifier.create(registrationService.register(validRequest))
                .expectErrorMatches(ex ->
                        ex instanceof RegistrationException &&
                                ex.getMessage().contains("Failed to create user"))
                .verify();
    }

    @Test
    void register_ShouldRollbackUser_WhenAuthServiceReturns500() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(userResponse)));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(204));

        StepVerifier.create(registrationService.register(validRequest))
                .expectErrorMatches(ex ->
                        ex instanceof RegistrationException &&
                                ex.getMessage().contains("Failed to save credentials"))
                .verify();

        RecordedRequest createRequest = mockWebServer.takeRequest();
        assertThat(createRequest.getPath()).contains("/api/users/register");

        RecordedRequest credentialsRequest = mockWebServer.takeRequest();
        assertThat(credentialsRequest.getPath()).contains("/api/auth/save-credentials");

        RecordedRequest deleteRequest = mockWebServer.takeRequest();
        assertThat(deleteRequest.getMethod()).isEqualTo("DELETE");
        assertThat(deleteRequest.getPath()).contains("/api/users/internal/1");
        assertThat(deleteRequest.getHeader("X-Internal-Api-Key")).isEqualTo(INTERNAL_API_KEY);
    }

    @Test
    void register_ShouldThrowRegistrationException_WhenCredentialsAlreadyExist() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(userResponse)));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(409));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(204));

        StepVerifier.create(registrationService.register(validRequest))
                .expectErrorMatches(ex ->
                        ex instanceof RegistrationException &&
                                ex.getMessage().contains("Credentials are already exist"))
                .verify();
    }

    @Test
    void register_ShouldPropagateOriginalError_EvenWhenRollbackFails() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(userResponse)));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500));

        StepVerifier.create(registrationService.register(validRequest))
                .expectErrorMatches(ex ->
                        ex instanceof RegistrationException &&
                                ex.getMessage().contains("Failed to save credentials"))
                .verify();
    }

    @Test
    void register_ShouldSendCorrectUserData() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(userResponse)));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201));

        registrationService.register(validRequest).block();

        RecordedRequest request = mockWebServer.takeRequest();
        String body = request.getBody().readUtf8();

        assertThat(body)
                .contains("john@test.com")
                .contains("John")
                .contains("Doe");
    }

    @Test
    void register_ShouldSendInternalApiKey_WhenSavingCredentials() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(userResponse)));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201));

        registrationService.register(validRequest).block();

        mockWebServer.takeRequest();
        RecordedRequest credentialsRequest = mockWebServer.takeRequest();

        assertThat(credentialsRequest.getHeader("X-Internal-Api-Key"))
                .isEqualTo(INTERNAL_API_KEY);
    }

    @Test
    void register_ShouldSendRoleUser_WhenSavingCredentials() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(userResponse)));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201));

        registrationService.register(validRequest).block();

        mockWebServer.takeRequest();
        RecordedRequest credentialsRequest = mockWebServer.takeRequest();
        String body = credentialsRequest.getBody().readUtf8();

        assertThat(body).contains("ROLE_USER");
    }
}