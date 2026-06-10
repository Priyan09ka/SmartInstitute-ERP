package com.smartinstitute.erp.academic.teacher.repository;

import com.smartinstitute.erp.academic.teacher.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    List<Teacher> findByInstitute_Id(Long instituteId);

    @Query("SELECT t FROM Teacher t JOIN FETCH t.user WHERE t.institute.id = :instituteId")
    List<Teacher> findByInstitute_IdJoinUser(@Param("instituteId") Long instituteId);

    Optional<Teacher> findByIdAndInstitute_Id(Long id,Long instituteId);

    Optional<Teacher> findByUser_Email(String email);

}
