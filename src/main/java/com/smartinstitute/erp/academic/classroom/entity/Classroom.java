package com.smartinstitute.erp.academic.classroom.entity;

import com.smartinstitute.erp.platform.institute.entity.Institute;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "classrooms",
        uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name","institute_id"})
})
@Data
@Filter(name = "tenantFilter",condition = "institute_id=:instituteId")
public class Classroom {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false)
        private String name; // e.g. "FY BCA", "10-A"

        @ManyToOne(optional = false)
        @JoinColumn(name = "institute_id")
        private Institute institute;

}
