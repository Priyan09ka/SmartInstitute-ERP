package com.smartinstitute.erp.academic.student.dto;

import lombok.Data;

@Data
public class StudentUpdateRequestDto {
    private String name;
    private String email;
    private Long classroomId;
    private String rollNumber;
}