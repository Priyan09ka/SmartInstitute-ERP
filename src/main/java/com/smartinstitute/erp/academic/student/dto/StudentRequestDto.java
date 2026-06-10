package com.smartinstitute.erp.academic.student.dto;

import lombok.Data;

@Data
public class StudentRequestDto {
    private String name;
    private String email;
    private String password;
    private String rollNumber;
    private long classroomId;
}
