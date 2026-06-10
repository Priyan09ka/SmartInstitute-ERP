package com.smartinstitute.erp.academic.quiz.repository;

import com.smartinstitute.erp.academic.quiz.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    List<QuizQuestion> findByQuiz_Id(Long quizId);

}