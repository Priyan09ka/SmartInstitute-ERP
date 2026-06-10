package com.smartinstitute.erp.platform.institute.dto;

import lombok.Data;

@Data
public class InstituteResponseDto {

    private Long id;
    private String name;
    private String address;
    private String phone;
    private String email;
    private String logoUrl;
    private boolean active;
}