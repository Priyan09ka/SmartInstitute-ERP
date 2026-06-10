package com.smartinstitute.erp.user.repository;

import com.smartinstitute.erp.platform.institute.entity.Institute;
import com.smartinstitute.erp.user.entity.User;
import com.smartinstitute.erp.user.entity.UserInstitute;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserInstituteRepository extends JpaRepository<UserInstitute,Long> {
    Optional<UserInstitute> findFirstByUser(User user);

    List<UserInstitute> findAllByUserOrderByIdAsc(User user);

    Optional<UserInstitute> findByUserAndInstitute(User user, Institute institute);

  Optional<UserInstitute> findByUserEmail(String email);
}
