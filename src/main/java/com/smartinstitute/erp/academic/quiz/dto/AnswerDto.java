package com.smartinstitute.erp.academic.quiz.dto;

import lombok.Data;

@Data
public class AnswerDto {

    private Long questionId;
    private String selectedAnswer;
}
