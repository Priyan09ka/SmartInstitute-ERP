package com.smartinstitute.erp.academic.quiz.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table
@Data
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String question;

    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    private String correctAnswer;

    private Integer marks;

    @ManyToOne
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;
}