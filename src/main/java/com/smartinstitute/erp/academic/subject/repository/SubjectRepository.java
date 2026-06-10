package com.smartinstitute.erp.academic.subject.repository;

import com.smartinstitute.erp.academic.subject.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findByInstitute_Id(Long instituteId);

    boolean existsByNameAndCourse_IdAndInstitute_Id(
            String name, Long courseId, Long instituteId);

}