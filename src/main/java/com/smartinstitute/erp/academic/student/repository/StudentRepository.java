package com.smartinstitute.erp.academic.student.repository;

import com.smartinstitute.erp.academic.classroom.entity.Classroom;
import com.smartinstitute.erp.academic.student.entity.Student;
import com.smartinstitute.erp.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student,Long> {

    List<Student> findAllByInstitute_Id(Long instituteId);

    @Query("SELECT s FROM Student s JOIN FETCH s.user WHERE s.institute.id = :instituteId")
    List<Student> findAllByInstitute_IdJoinUser(@Param("instituteId") Long instituteId);

    Optional<Student> findByIdAndInstitute_Id(Long id, Long instituteId);

    Optional<Student> findByUser(User user);

    Optional<Student> findByUser_Email(String email);

    boolean existsByUser(User user);

    boolean existsByClassroomAndRollNumberAndInstitute_Id(Classroom classroom, String rollNumber, Long instituteId);

    boolean existsByUser_EmailAndInstitute_Id(String email, Long instituteId);

    List<Student> findByInstitute_IdAndClassroom_IdIn(Long instituteId, List<Long> classroomIds);
}
