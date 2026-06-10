package com.smartinstitute.erp.academic.teachersubject.dto;

import lombok.Data;

@Data
public class TeacherSubjectResponseDto {

    private Long id;

    private Long teacherId;
    private String teacherName;

    private Long subjectId;
    private String subjectName;

}
