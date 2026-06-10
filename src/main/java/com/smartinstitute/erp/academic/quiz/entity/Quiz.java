package com.smartinstitute.erp.academic.quiz.entity;

import com.smartinstitute.erp.academic.classroom.entity.Classroom;
import com.smartinstitute.erp.academic.subject.entity.Subject;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private Integer totalMarks;

    private Integer durationMinutes;

    @ManyToOne
    private Subject subject;

    @ManyToOne
    private Classroom classroom;
}
