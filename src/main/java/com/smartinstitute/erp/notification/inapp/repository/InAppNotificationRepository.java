package com.smartinstitute.erp.notification.inapp.repository;

import com.smartinstitute.erp.notification.inapp.entity.InAppNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {

    List<InAppNotification> findByUser_IdAndInstitute_IdOrderByCreatedAtDesc(Long userId, Long instituteId);

    long countByUser_IdAndInstitute_IdAndReadFlag(Long userId, Long instituteId, boolean readFlag);
}
