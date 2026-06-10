package com.smartinstitute.erp.academic.principal.dto;

import lombok.Data;

@Data
public class PrincipalResponseDto {

    private Long id;
    private long userId;
    private String name;
    private String email;
    private String instituteName;

}