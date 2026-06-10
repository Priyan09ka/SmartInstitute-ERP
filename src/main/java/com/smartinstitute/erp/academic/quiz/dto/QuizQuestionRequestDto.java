package com.smartinstitute.erp.academic.quiz.dto;

import lombok.Data;
@Data
public class QuizQuestionRequestDto {

    private Long quizId;

    private String question;

    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    private String correctAnswer;

    private int marks;
}