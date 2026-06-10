package com.smartinstitute.erp.academic.student.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponseDto {
    private Long id;
    private Long classroomId;
    private Long userId;
    private String name;
    private String email;
    private String rollNumber;
    private String classroomName;
}
