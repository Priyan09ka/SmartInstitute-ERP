package com.smartinstitute.erp.academic.fee.repository;

import com.smartinstitute.erp.academic.fee.entity.FeeRecord;
import com.smartinstitute.erp.academic.fee.entity.FeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeeRecordRepository extends JpaRepository<FeeRecord, Long> {
    List<FeeRecord> findAllByInstitute_IdOrderByDueDateAsc(Long instituteId);
    List<FeeRecord> findAllByInstitute_IdAndStudent_IdOrderByDueDateAsc(Long instituteId, Long studentId);
    List<FeeRecord> findAllByInstitute_IdAndStatusOrderByDueDateAsc(Long instituteId, FeeStatus status);
    Optional<FeeRecord> findByIdAndInstitute_IdAndStudent_Id(Long id, Long instituteId, Long studentId);
}
