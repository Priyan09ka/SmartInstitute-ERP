package com.smartinstitute.erp.academic.quiz.controller;

import com.smartinstitute.erp.academic.quiz.dto.*;
import com.smartinstitute.erp.academic.quiz.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @GetMapping
    public List<QuizResponseDto> listQuizzes() {
        return quizService.listQuizzes();
    }

    @PostMapping
    public QuizResponseDto createQuiz(@RequestBody QuizRequestDto dto){
        return quizService.createQuiz(dto);
    }

    @GetMapping("/{quizId}/questions")
    public List<QuizQuestionResponseDto> getQuestions(@PathVariable Long quizId){
        return quizService.getQuizQuestions(quizId);
    }
    @PutMapping("/{id}")
    public QuizResponseDto updateQuiz(@PathVariable Long id,
                                      @RequestBody QuizRequestDto dto){
        return quizService.updateQuiz(id, dto);
    }

    @DeleteMapping("/{id}")
    public String deleteQuiz(@PathVariable Long id){
        quizService.deleteQuiz(id);
        return "Quiz deleted successfully";
    }

    @PostMapping("/{quizId}/submit")
    public QuizResultResponseDto submitQuiz(
            @PathVariable Long quizId,
            @RequestBody QuizSubmitRequestDto dto){

        return quizService.submitQuiz(
                quizId,
                dto.getStudentId(),
                dto.getAnswers());
    }

    @GetMapping("/results/{studentId}")
    public List<QuizResultResponseDto> getStudentResults(
            @PathVariable Long studentId){

        return quizService.getStudentResults(studentId);
    }
}