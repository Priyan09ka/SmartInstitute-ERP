package com.smartinstitute.erp.academic.course.entity;

import com.smartinstitute.erp.platform.institute.entity.Institute;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Filter;

@Entity
@Table(
        name = "courses",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"name", "institute_id"})
        }
)
@Data
@Filter(name = "tenantFilter", condition = "institute_id = :instituteId")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String code;

    private String description;

    @ManyToOne
    @JoinColumn(name = "institute_id")
    private Institute institute;
}