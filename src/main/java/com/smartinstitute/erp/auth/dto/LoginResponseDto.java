package com.smartinstitute.erp.auth.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginResponseDto {

    private Long id;
    private String email;
    private String role;
    private boolean success;
    private String message;
    private String token;
    private String refreshToken;

    // static helper for failure
    public static LoginResponseDto fail(String msg) {
        LoginResponseDto dto = new LoginResponseDto();
        dto.setSuccess(false);
        dto.setMessage(msg);
        return dto;
    }

    // static helper for success
    public static LoginResponseDto success(
            Long id,
            String email,
            String role,
            String token,
            String refreshToken

    ) {
        LoginResponseDto dto = new LoginResponseDto();
        dto.setId(id);
        dto.setEmail(email);
        dto.setRole(role);
        dto.setToken(token);
        dto.setRefreshToken(refreshToken);
        dto.setSuccess(true);
        dto.setMessage("Login successful");
        return dto;
    }
}
