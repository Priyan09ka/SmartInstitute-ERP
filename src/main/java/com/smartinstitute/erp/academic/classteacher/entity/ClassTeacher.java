package com.smartinstitute.erp.academic.classteacher.entity;

import com.smartinstitute.erp.academic.classroom.entity.Classroom;
import com.smartinstitute.erp.academic.teacher.entity.Teacher;
import com.smartinstitute.erp.platform.institute.entity.Institute;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Filter;

@Entity
@Table(
        name = "class_teachers",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"classroom_id","institute_id"})
        }
)
@Data
@Filter(name = "tenantFilter", condition = "institute_id = :instituteId")
public class ClassTeacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "classroom_id")
    private Classroom classroom;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @ManyToOne
    @JoinColumn(name = "institute_id")
    private Institute institute;
}