package com.goylik.api_gateway.model.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank @Size(max = 127) String name,
        @NotBlank @Size(max = 127) String surname,
        @NotNull @Past LocalDate birthDate,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 255) String password
) {
}
