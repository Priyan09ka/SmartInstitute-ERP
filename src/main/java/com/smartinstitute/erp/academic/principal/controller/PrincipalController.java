package com.smartinstitute.erp.academic.principal.controller;

import com.smartinstitute.erp.academic.principal.dto.PrincipalRequestDto;
import com.smartinstitute.erp.academic.principal.dto.PrincipalResponseDto;
import com.smartinstitute.erp.academic.principal.service.PrincipalService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/principals")
@RequiredArgsConstructor
public class PrincipalController {

    private final PrincipalService principalService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTITUTE_ADMIN')")
    public PrincipalResponseDto create(@RequestBody PrincipalRequestDto dto) {
        return principalService.create(dto);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTITUTE_ADMIN','PRINCIPAL')")
    public List<PrincipalResponseDto> list() {
        return principalService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTITUTE_ADMIN','PRINCIPAL')")
    public PrincipalResponseDto get(@PathVariable Long id) {
        return principalService.get(id);
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTITUTE_ADMIN')")
    public String deactivate(@PathVariable Long id) {
        principalService.deactivatePrincipal(id);
        return "Principal is deactivated successfully!!";
    }
}