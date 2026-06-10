package com.smartinstitute.erp.academic.classroom.controller;

import com.smartinstitute.erp.academic.classroom.dto.ClassroomRequestDto;
import com.smartinstitute.erp.academic.classroom.dto.ClassroomResponseDto;
import com.smartinstitute.erp.academic.classroom.service.ClassroomService;
import com.smartinstitute.erp.academic.teacher.service.TeacherPermissionService;
import com.smartinstitute.erp.auth.entity.TenantContext;
import com.smartinstitute.erp.platform.institute.entity.Institute;
import com.smartinstitute.erp.platform.institute.repository.InstituteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/institute/classes")
@RequiredArgsConstructor
public class ClassroomController {

    private final ClassroomService service;
    private final InstituteRepository instituteRepo;
    private final TeacherPermissionService teacherPermissionService;

    private Institute currentInstitute(Long id) {

        if (id == null) {
            throw new RuntimeException("Tenant not resolved from token");
        }

        return instituteRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Institute not found"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTITUTE_ADMIN','PRINCIPAL')")
    public ClassroomResponseDto create(@RequestBody ClassroomRequestDto dto) {
        return service.create(dto, currentInstitute(TenantContext.getTenantId()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('INSTITUTE_ADMIN','PRINCIPAL','TEACHER')")
    public List<ClassroomResponseDto> list() {
        Institute inst = currentInstitute(TenantContext.getTenantId());
        if (teacherPermissionService.isTeacherRole()) {
            return service.listForTeacher(inst);
        }
        return service.list(inst);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTITUTE_ADMIN','PRINCIPAL','TEACHER')")
    public ClassroomResponseDto get(@PathVariable Long id) {
        Institute inst = currentInstitute(TenantContext.getTenantId());
        if (teacherPermissionService.isTeacherRole()) {
            teacherPermissionService.assertTeacherCanAccessClassroom(id);
        }
        return service.get(id, inst);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTITUTE_ADMIN','PRINCIPAL')")
    public ClassroomResponseDto update(
            @PathVariable Long id,
            @RequestBody ClassroomRequestDto dto
    ) {
        return service.update(id, dto, currentInstitute(TenantContext.getTenantId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTITUTE_ADMIN','PRINCIPAL')")
    public void delete(@PathVariable Long id) {
        service.delete(id, currentInstitute(TenantContext.getTenantId()));
    }
}
