package com.smartinstitute.erp.platform.admin.controller;

import com.smartinstitute.erp.platform.admin.dto.InstituteUsersResponseDto;
import com.smartinstitute.erp.platform.admin.service.SuperAdminService;
import com.smartinstitute.erp.platform.institute.entity.Institute;
import com.smartinstitute.erp.platform.institute.entity.InstituteRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/platform/institutes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")

public class SuperAdminController {

    private final SuperAdminService service;

    //  Get all institutes
    @GetMapping
    public List<Institute> getAllInstitutes() {
        return service.getAllInstitutes();
    }

    // Toggle active/inactive
    @PutMapping("/{id}/toggle")
    public void toggleInstitute(@PathVariable Long id) {
        service.toggleInstitute(id);
    }

    // Get users (students, teachers, principals) for an institute
    @GetMapping("/{instituteId}/users")
    public InstituteUsersResponseDto getInstituteUsers(@PathVariable Long instituteId) {
        return service.getInstituteUsers(instituteId);
    }

    // Get pending requests
    @GetMapping("/requests")
    public List<InstituteRequest> getRequests() {
        return service.getPendingRequests();
    }

    // Approve request
    @PostMapping("/requests/{id}/approve")
    public void approve(@PathVariable Long id) {
        service.approveRequest(id);
    }

    // Reject request
    @PostMapping("/requests/{id}/reject")
    public void reject(@PathVariable Long id) {
        service.rejectRequest(id);
    }
}
