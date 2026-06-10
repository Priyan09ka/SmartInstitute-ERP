package com.smartinstitute.erp.academic.quiz.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuizQuestionResponseDto {

    private Long id;

    private String question;

    private List<String> options;

    private int marks;
}