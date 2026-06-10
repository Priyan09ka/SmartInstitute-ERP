package com.smartinstitute.erp.academic.teachersubject.entity;

import com.smartinstitute.erp.academic.subject.entity.Subject;
import com.smartinstitute.erp.academic.teacher.entity.Teacher;
import com.smartinstitute.erp.platform.institute.entity.Institute;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Filter;

@Entity
@Table(
        name = "teacher_subjects",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"teacher_id","subject_id","institute_id"})
        }
)
@Data
@Filter(name = "tenantFilter", condition = "institute_id = :instituteId")
public class TeacherSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "institute_id")
    private Institute institute;
}
