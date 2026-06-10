package com.smartinstitute.erp.academic.schedule.repository;

import com.smartinstitute.erp.academic.schedule.entity.ScheduleSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, Long> {

    /**
     * Simple query only — do not JOIN FETCH other @Filter entities here: Hibernate applies the same
     * tenant filter to every joined alias and can produce invalid SQL / 500 errors.
     */
    List<ScheduleSlot> findByInstitute_Id(Long instituteId);

    List<ScheduleSlot> findByInstitute_IdAndClassroom_Id(Long instituteId, Long classroomId);

    boolean existsByInstitute_IdAndClassroom_IdAndTeacher_Id(Long instituteId, Long classroomId, Long teacherId);
}
