package com.smartinstitute.erp.academic.student.dto;

import lombok.Data;

@Data
public class StudentMeDto {
    private long id;
    private long userId;
    private String name;
    private String email;
    private String rollNumber;
    private long classroomId;
    private String classroomName;
    private String instituteName;
}
