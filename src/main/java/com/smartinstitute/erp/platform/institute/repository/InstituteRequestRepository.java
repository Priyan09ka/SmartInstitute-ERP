package com.smartinstitute.erp.platform.institute.repository;

import com.smartinstitute.erp.platform.institute.RequestStatus;
import com.smartinstitute.erp.platform.institute.entity.InstituteRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstituteRequestRepository
        extends JpaRepository<InstituteRequest, Long> {

    List<InstituteRequest> findByStatus(RequestStatus status);
}