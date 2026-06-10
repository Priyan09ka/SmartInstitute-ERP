package com.smartinstitute.erp.user.dto;

import lombok.Data;

@Data
public class LoginResponseDto {
    private Long id;
    private String email;
    private String role;
    private boolean success;
    private String message;
    private String token;
    private String refreshToken;
}