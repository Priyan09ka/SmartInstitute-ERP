package com.smartinstitute.erp.academic.quiz.controller;

import com.smartinstitute.erp.academic.quiz.dto.*;
import com.smartinstitute.erp.academic.quiz.service.QuizQuestionBulkImportService;
import com.smartinstitute.erp.academic.quiz.service.QuizQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping({"/api/questions", "/institute/quiz-questions"})
@RequiredArgsConstructor
public class QuizQuestionController {

    private final QuizQuestionService questionService;
    private final QuizQuestionBulkImportService bulkImportService;

    /** Register before bare {@code @PostMapping} so paths are not mis-resolved. */
    @PostMapping("/import/csv")
    public QuizQuestionImportResultDto importCsv(
            @RequestParam Long quizId,
            @RequestParam("file") MultipartFile file) {
        return bulkImportService.importFromCsv(quizId, file);
    }

    @PostMapping("/import/pdf")
    public QuizQuestionImportResultDto importPdf(
            @RequestParam Long quizId,
            @RequestParam("file") MultipartFile file) {
        return bulkImportService.importFromPdf(quizId, file);
    }

    @PostMapping
    public QuizQuestionResponseDto addQuestion(
            @RequestBody QuizQuestionRequestDto dto){

        return questionService.addQuestion(dto);
    }

    @GetMapping("/quiz/{quizId}")
    public List<QuizQuestionResponseDto> getQuestionsByQuiz(
            @PathVariable Long quizId){

        return questionService.getQuestionsByQuiz(quizId);
    }

    @PutMapping("/{id}")
    public QuizQuestionResponseDto updateQuestion(
            @PathVariable Long id,
            @RequestBody QuizQuestionRequestDto dto){

        return questionService.updateQuestion(id, dto);
    }

    @DeleteMapping("/{id}")
    public String deleteQuestion(@PathVariable Long id){

        questionService.deleteQuestion(id);

        return "Quiz question deleted successfully";
    }

}