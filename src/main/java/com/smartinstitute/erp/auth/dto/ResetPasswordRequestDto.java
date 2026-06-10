package com.smartinstitute.erp.auth.dto;

import lombok.Data;

@Data
public class ResetPasswordRequestDto {
    private String email;
    private String code;
    private String newPassword;
}
