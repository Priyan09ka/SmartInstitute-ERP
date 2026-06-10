package com.smartinstitute.erp.academic.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionImportResultDto {

    private int importedCount;
    private int failedCount;
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
