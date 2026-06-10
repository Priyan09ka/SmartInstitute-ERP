package com.smartinstitute.erp.notification.inapp.controller;

import com.smartinstitute.erp.auth.service.AuthService;
import com.smartinstitute.erp.notification.inapp.dto.InAppNotificationDto;
import com.smartinstitute.erp.notification.inapp.service.InAppNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class InAppNotificationController {

    private final InAppNotificationService notificationService;
    private final AuthService authService;

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public List<InAppNotificationDto> list() {
        return notificationService.listForCurrentUser(authService.getCurrentUser());
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasRole('STUDENT')")
    public void markRead(@PathVariable Long id) {
        notificationService.markRead(id, authService.getCurrentUser());
    }
}
