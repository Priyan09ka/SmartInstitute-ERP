package com.smartinstitute.erp.academic.fee.repository;

import com.smartinstitute.erp.academic.fee.entity.FeeGatewayTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeeGatewayTransactionRepository extends JpaRepository<FeeGatewayTransaction, Long> {
    Optional<FeeGatewayTransaction> findByGatewayOrderIdAndInstitute_Id(String gatewayOrderId, Long instituteId);
}
