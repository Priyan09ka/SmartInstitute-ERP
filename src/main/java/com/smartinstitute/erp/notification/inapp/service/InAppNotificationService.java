package com.smartinstitute.erp.notification.inapp.service;

import com.smartinstitute.erp.auth.entity.TenantContext;
import com.smartinstitute.erp.notification.inapp.dto.InAppNotificationDto;
import com.smartinstitute.erp.notification.inapp.entity.InAppNotification;
import com.smartinstitute.erp.notification.inapp.repository.InAppNotificationRepository;
import com.smartinstitute.erp.platform.institute.entity.Institute;
import com.smartinstitute.erp.platform.institute.repository.InstituteRepository;
import com.smartinstitute.erp.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InAppNotificationService {

    private final InAppNotificationRepository repo;
    private final InstituteRepository instituteRepository;

    @Transactional
    public void notifyUser(User user, String message) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null || user == null) {
            return;
        }
        Institute institute = instituteRepository.findById(tenantId)
                .orElse(null);
        if (institute == null) {
            return;
        }
        InAppNotification n = new InAppNotification();
        n.setUser(user);
        n.setInstitute(institute);
        n.setMessage(message);
        n.setReadFlag(false);
        repo.save(n);
    }

    public List<InAppNotificationDto> listForCurrentUser(User currentUser) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Institute context missing");
        }
        return repo.findByUser_IdAndInstitute_IdOrderByCreatedAtDesc(currentUser.getId(), tenantId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void markRead(Long id, User currentUser) {
        Long tenantId = TenantContext.getTenantId();
        InAppNotification n = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (n.getUser().getId() != currentUser.getId()
                || tenantId == null
                || !n.getInstitute().getId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your notification");
        }
        n.setReadFlag(true);
    }

    private InAppNotificationDto toDto(InAppNotification n) {
        InAppNotificationDto d = new InAppNotificationDto();
        d.setId(n.getId());
        d.setMessage(n.getMessage());
        d.setRead(n.isReadFlag());
        d.setCreatedAt(n.getCreatedAt());
        return d;
    }
}
