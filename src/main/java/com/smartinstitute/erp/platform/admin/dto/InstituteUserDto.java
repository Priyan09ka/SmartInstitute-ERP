package com.smartinstitute.erp.platform.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstituteUserDto {
    private Long id;
    private String name;
    private String email;
    private String role; // STUDENT, TEACHER, PRINCIPAL
}
