package com.smartinstitute.erp.academic.quiz.dto;

import lombok.Data;

@Data
public class QuizResponseDto {

    private Long id;
    private String title;
    private Integer totalMarks;
    private Integer durationMinutes;

    private String subjectName;
    private String classroomName;
}