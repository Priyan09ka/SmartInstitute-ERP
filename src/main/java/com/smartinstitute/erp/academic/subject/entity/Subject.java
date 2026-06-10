package com.smartinstitute.erp.academic.subject.entity;

import com.smartinstitute.erp.academic.course.entity.Course;
import com.smartinstitute.erp.platform.institute.entity.Institute;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "subjects", uniqueConstraints = {
        @UniqueConstraint(columnNames = {
                "name","course_id","institute_id"
        })
})
@Data
@Filter(name = "tenantFilter",condition = "institute_id=:instituteId")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String code;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne
    @JoinColumn(name = "institute_id")
    private Institute institute;

}
