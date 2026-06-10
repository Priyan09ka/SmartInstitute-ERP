package com.smartinstitute.erp.platform.institute.controller;

import com.smartinstitute.erp.platform.institute.dto.InstituteResponseDto;
import com.smartinstitute.erp.platform.institute.service.InstituteAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform/institute-admin")
@RequiredArgsConstructor
public class InstituteAdminController {

    private final InstituteAdminService service;

    @GetMapping("/my-institute")
    @PreAuthorize("hasAnyRole('INSTITUTE_ADMIN','PRINCIPAL')")
    public InstituteResponseDto getMyInstitute() {
        return service.getMyInstitute();
    }

    @PutMapping("/update")
    @PreAuthorize("hasRole('INSTITUTE_ADMIN')")
    public InstituteResponseDto update(@RequestBody InstituteResponseDto dto) {
        return service.updateInstitute(dto);
    }
}