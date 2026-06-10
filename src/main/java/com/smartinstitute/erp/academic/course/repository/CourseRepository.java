package com.smartinstitute.erp.academic.course.repository;

import com.smartinstitute.erp.academic.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByInstitute_Id(Long instituteId);

    Optional<Course> findByIdAndInstitute_Id(Long id, Long instituteId);
    boolean existsByNameAndInstitute_Id(String name, Long instituteId);
}