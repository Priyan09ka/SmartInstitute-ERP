package com.smartinstitute.erp.academic.classteacher.dto;

import lombok.Data;

@Data
public class ClassTeacherResponseDto {

    private Long id;

    private Long classroomId;
    private String classroomName;

    private Long teacherId;
    private String teacherName;

}
