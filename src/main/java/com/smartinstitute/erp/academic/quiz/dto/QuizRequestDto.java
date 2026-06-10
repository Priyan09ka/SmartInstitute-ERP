package com.smartinstitute.erp.academic.quiz.dto;

import lombok.Data;

@Data
public class QuizRequestDto {

    private String title;
    private int totalMarks;
    private int durationMinutes;

    private Long subjectId;
    private Long classroomId;
}