package com.smartinstitute.erp.academic.teacher.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TeacherResponseDto {
    private long id;
    private long userId;
    private String name;
    private String email;
    private String role;
    private String status;
    private String instituteName;
    /** Classes where this teacher is the assigned class teacher (can add/import students). */
    private List<Long> homeroomClassroomIds = new ArrayList<>();
    /** Has at least one subject assignment in this institute. */
    private boolean assignedAsSubjectTeacher;
}
