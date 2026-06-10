package com.smartinstitute.erp.academic.fee.repository;

import com.smartinstitute.erp.academic.fee.entity.FeePayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeePaymentRepository extends JpaRepository<FeePayment, Long> {
    List<FeePayment> findAllByFeeRecord_IdAndInstitute_IdOrderByPaidAtDesc(Long feeRecordId, Long instituteId);
}
