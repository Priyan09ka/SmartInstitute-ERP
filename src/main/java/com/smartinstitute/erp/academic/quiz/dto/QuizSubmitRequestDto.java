package com.smartinstitute.erp.academic.quiz.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuizSubmitRequestDto {

    private Long studentId;

    private List<AnswerDto> answers;
}