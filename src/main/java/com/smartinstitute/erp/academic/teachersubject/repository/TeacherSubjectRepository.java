package com.smartinstitute.erp.academic.teachersubject.repository;

import com.smartinstitute.erp.academic.teachersubject.entity.TeacherSubject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeacherSubjectRepository extends JpaRepository<TeacherSubject, Long> {

    List<TeacherSubject> findByInstitute_Id(Long instituteId);

    boolean existsByTeacher_IdAndSubject_Id(Long teacherId, Long subjectId);

    List<TeacherSubject> findByTeacher_Id(Long teacherId);
}
