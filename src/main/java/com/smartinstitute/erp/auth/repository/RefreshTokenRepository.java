package com.smartinstitute.erp.auth.repository;

import com.smartinstitute.erp.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void deleteByUserEmail(String userEmail);
}
