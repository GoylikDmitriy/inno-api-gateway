package com.goylik.api_gateway.model.dto.request;

import com.goylik.api_gateway.model.enums.Role;

public record SaveCredentialsRequest(
        Long userId,
        String email,
        String password,
        Role role
) {
}
