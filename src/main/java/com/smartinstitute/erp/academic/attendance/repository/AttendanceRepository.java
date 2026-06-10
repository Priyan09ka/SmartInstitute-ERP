package com.smartinstitute.erp.academic.attendance.repository;

import com.smartinstitute.erp.academic.attendance.entity.Attendance;
import com.smartinstitute.erp.academic.attendance.entity.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance,Long> {

    List<Attendance> findByClassroom_IdAndDate(Long classroomId, LocalDate date);

    Optional<Attendance> findByStudent_IdAndDateAndInstitute_Id(
            Long studentId, LocalDate date, Long instituteId);

    long countByStudent_Id(Long studentId);

    long countByStudent_IdAndStatus(Long studentId, AttendanceStatus status);

    long countByInstitute_IdAndDateAndStatus(Long instituteId, LocalDate date, AttendanceStatus status);

    List<Attendance> findByStudent_IdOrderByDateDesc(Long studentId);
}
