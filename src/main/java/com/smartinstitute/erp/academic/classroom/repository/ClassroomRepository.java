package com.smartinstitute.erp.academic.classroom.repository;

import com.smartinstitute.erp.academic.classroom.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClassroomRepository extends JpaRepository<Classroom,Long> {
    boolean existsByNameAndInstitute_Id(String name, Long instituteId);

    List<Classroom> findAllByInstitute_Id(Long instituteId);

    List<Classroom> findByInstitute_IdAndIdIn(Long instituteId, Collection<Long> ids);

    Optional<Classroom> findByIdAndInstitute_Id(Long id, Long instituteId);
}
