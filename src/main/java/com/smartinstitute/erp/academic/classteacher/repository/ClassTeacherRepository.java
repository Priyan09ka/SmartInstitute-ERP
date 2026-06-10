package com.smartinstitute.erp.academic.classteacher.repository;

import com.smartinstitute.erp.academic.classteacher.entity.ClassTeacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassTeacherRepository extends JpaRepository<ClassTeacher, Long> {

    List<ClassTeacher> findByInstitute_Id(Long instituteId);

    List<ClassTeacher> findByTeacher_Id(Long teacherId);

    Optional<ClassTeacher> findByTeacher_IdAndClassroom_Id(Long teacherId, Long classroomId);

    boolean existsByTeacher_IdAndClassroom_IdAndInstitute_Id(Long teacherId, Long classroomId, Long instituteId);
}
