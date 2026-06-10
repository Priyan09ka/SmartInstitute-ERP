package com.smartinstitute.erp.academic.attendance.entity;

import com.smartinstitute.erp.academic.classroom.entity.Classroom;
import com.smartinstitute.erp.academic.student.entity.Student;
import com.smartinstitute.erp.platform.institute.entity.Institute;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Filter;

import java.time.LocalDate;

@Entity
@Table(
        name = "attendance",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"student_id","date","institute_id"})
        }
)
@Data
@Filter(name = "tenantFilter", condition = "institute_id=:instituteId")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="student_id")
    private Student student;

    @ManyToOne
    @JoinColumn(name="classroom_id")
    private Classroom classroom;

    @ManyToOne
    @JoinColumn(name="institute_id")
    private Institute institute;

    private LocalDate date;

    /** VARCHAR so values like HOLIDAY are not limited by a legacy MySQL ENUM definition. */
    @Column(nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private AttendanceStatus status;
}
