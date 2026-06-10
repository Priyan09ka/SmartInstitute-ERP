package com.smartinstitute.erp.academic.quiz.dto;

import lombok.Data;

@Data
public class QuizResultResponseDto {

    private Long studentId;
    private Long quizId;

    private Integer score;
    private Integer totalMarks;

    private Double percentage;
}