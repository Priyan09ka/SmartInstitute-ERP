package com.smartinstitute.erp.academic.student.entity;

import com.smartinstitute.erp.academic.classroom.entity.Classroom;
import com.smartinstitute.erp.platform.institute.entity.Institute;
import com.smartinstitute.erp.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "students",
uniqueConstraints = {
        @UniqueConstraint(columnNames = {
                "classroom_id","roll_no","institute_id"
        })
})
@Data
@Filter(name = "tenantFilter",condition = "institute_id=:instituteId")

public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @Column(name = "roll_no", nullable = false)
    private String rollNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "institute_id", nullable = false)
    private Institute institute;
}

