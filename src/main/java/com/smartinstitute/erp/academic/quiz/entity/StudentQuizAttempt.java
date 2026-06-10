package com.smartinstitute.erp.academic.quiz.entity;

import com.smartinstitute.erp.academic.student.entity.Student;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class StudentQuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Student student;

    @ManyToOne
    private Quiz quiz;

    private Integer score;

    private Integer totalMarks;

    private Double percentage;
}
