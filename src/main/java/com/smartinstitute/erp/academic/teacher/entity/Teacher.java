package com.smartinstitute.erp.academic.teacher.entity;

import com.smartinstitute.erp.platform.institute.entity.Institute;
import com.smartinstitute.erp.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "teachers")
@Data
@Filter(name = "tenantFilter",condition = "institute_id=:instituteId")
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "institute_id")
    private Institute institute;

}
