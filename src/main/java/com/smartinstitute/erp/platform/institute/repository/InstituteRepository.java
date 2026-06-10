package com.smartinstitute.erp.platform.institute.repository;

import com.smartinstitute.erp.platform.institute.entity.Institute;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstituteRepository extends JpaRepository<Institute,Long> {
    boolean existsByAddress(String address);

}
