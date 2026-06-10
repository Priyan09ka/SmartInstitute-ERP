package com.smartinstitute.erp.platform.institute.dto;

import lombok.Data;

@Data
public class InstituteRequestDto {
    private String instituteName;
    private String adminName;
    private String adminEmail;
    private String password;
    /** Must match {@code password} when a password is set; not persisted. */
    private String confirmPassword;
    private String phone;
    private String logoUrl;
    private String address;

}
