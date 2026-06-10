package com.smartinstitute.erp.academic.teacher.dto;

import lombok.Data;

@Data
public class TeacherRequestDto {
    private String name;
    private String email;
    private String password;
    private String confirmPassword;
}
