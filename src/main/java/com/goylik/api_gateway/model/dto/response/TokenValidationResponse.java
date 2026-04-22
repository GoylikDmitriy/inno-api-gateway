package com.goylik.api_gateway.model.dto.response;

import com.goylik.api_gateway.model.enums.Role;

public record TokenValidationResponse(
        boolean valid,
        Long userId,
        Role role
) {
    public static TokenValidationResponse success(Long userId, Role role) {
        return new TokenValidationResponse(true, userId, role);
    }

    public static TokenValidationResponse failure() {
        return new TokenValidationResponse(false, null, null);
    }
}
