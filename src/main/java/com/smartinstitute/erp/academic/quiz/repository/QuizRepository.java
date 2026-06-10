package com.smartinstitute.erp.academic.quiz.repository;


import com.smartinstitute.erp.academic.quiz.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    @Query("SELECT q FROM Quiz q WHERE q.classroom.institute.id = :instituteId")
    List<Quiz> findByInstitute_Id(@Param("instituteId") Long instituteId);
}
