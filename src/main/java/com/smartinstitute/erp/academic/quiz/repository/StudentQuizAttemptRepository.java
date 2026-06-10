package com.smartinstitute.erp.academic.quiz.repository;

import com.smartinstitute.erp.academic.quiz.entity.StudentQuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentQuizAttemptRepository extends JpaRepository<StudentQuizAttempt, Long> {

    List<StudentQuizAttempt> findByStudent_Id(Long studentId);

}