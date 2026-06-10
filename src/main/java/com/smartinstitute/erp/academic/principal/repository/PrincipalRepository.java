package com.smartinstitute.erp.academic.principal.repository;

import com.smartinstitute.erp.academic.principal.entity.Principal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PrincipalRepository extends JpaRepository<Principal, Long> {

    List<Principal> findByInstitute_Id(Long instituteId);

    @Query("SELECT p FROM Principal p JOIN FETCH p.user WHERE p.institute.id = :instituteId")
    List<Principal> findByInstitute_IdJoinUser(@Param("instituteId") Long instituteId);

    Optional<Principal> findByIdAndInstitute_Id(Long id, Long instituteId);
}