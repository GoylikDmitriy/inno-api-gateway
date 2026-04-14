package com.goylik.api_gateway.model.dto.request;

import java.time.LocalDate;

public record CreateUserRequest(
        String name,
        String surname,
        LocalDate birthDate,
        String email
) {
}
