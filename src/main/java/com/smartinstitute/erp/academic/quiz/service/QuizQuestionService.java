package com.smartinstitute.erp.academic.quiz.service;

import com.smartinstitute.erp.academic.quiz.dto.*;
import com.smartinstitute.erp.academic.quiz.entity.*;
import com.smartinstitute.erp.academic.quiz.repository.*;
import com.smartinstitute.erp.academic.teacher.service.TeacherPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizQuestionService {

    private final TeacherPermissionService teacherPermissionService;
    private final QuizQuestionRepository questionRepository;
    private final QuizRepository quizRepository;

    public QuizQuestionResponseDto addQuestion(QuizQuestionRequestDto dto){

        Quiz quiz = quizRepository.findById(dto.getQuizId())
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        teacherPermissionService.assertCanManageQuiz(
                quiz.getSubject().getId(), quiz.getClassroom().getId());

        if(dto.getQuizId() == null){
            throw new RuntimeException("QuizId cannot be null");
        }

        QuizQuestion question = new QuizQuestion();

        question.setQuiz(quiz);
        question.setQuestion(dto.getQuestion());
        question.setOptionA(dto.getOptionA());
        question.setOptionB(dto.getOptionB());
        question.setOptionC(dto.getOptionC());
        question.setOptionD(dto.getOptionD());
        question.setCorrectAnswer(dto.getCorrectAnswer());
        question.setMarks(dto.getMarks());

        QuizQuestion saved = questionRepository.save(question);

        return toDto(saved);
    }

    public List<QuizQuestionResponseDto> getQuestionsByQuiz(Long quizId){

        return questionRepository.findByQuiz_Id(quizId)
                .stream()
                .map(this::toDto)
                .toList();
    }
    public QuizQuestionResponseDto updateQuestion(Long id, QuizQuestionRequestDto dto){

        QuizQuestion question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        Quiz quiz = question.getQuiz();
        teacherPermissionService.assertCanManageQuiz(
                quiz.getSubject().getId(), quiz.getClassroom().getId());

        question.setQuestion(dto.getQuestion());
        question.setOptionA(dto.getOptionA());
        question.setOptionB(dto.getOptionB());
        question.setOptionC(dto.getOptionC());
        question.setOptionD(dto.getOptionD());
        question.setCorrectAnswer(dto.getCorrectAnswer());
        question.setMarks(dto.getMarks());

        QuizQuestion updated = questionRepository.save(question);

        return toDto(updated);
    }

    public void deleteQuestion(Long id){

        QuizQuestion question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        Quiz quiz = question.getQuiz();
        teacherPermissionService.assertCanManageQuiz(
                quiz.getSubject().getId(), quiz.getClassroom().getId());

        questionRepository.delete(question);
    }

    private QuizQuestionResponseDto toDto(QuizQuestion question){

        QuizQuestionResponseDto dto = new QuizQuestionResponseDto();

        dto.setId(question.getId());
        dto.setQuestion(question.getQuestion());
        dto.setMarks(question.getMarks());

        List<String> options = new ArrayList<>();

        options.add(question.getOptionA());
        options.add(question.getOptionB());
        options.add(question.getOptionC());
        options.add(question.getOptionD());

        Collections.shuffle(options);

        dto.setOptions(options);

        return dto;
    }
}