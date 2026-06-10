package com.smartinstitute.erp.academic.principal.dto;

import lombok.Data;

@Data
public class PrincipalRequestDto {
    private String name;
    private String email;
    private String password;
    private String confirmPassword;
}
