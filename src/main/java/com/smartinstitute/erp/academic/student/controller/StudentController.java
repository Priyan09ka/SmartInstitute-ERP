package com.smartinstitute.erp.academic.student.controller;

import com.smartinstitute.erp.academic.student.dto.StudentRequestDto;
import com.smartinstitute.erp.academic.student.dto.StudentResponseDto;
import com.smartinstitute.erp.academic.student.dto.StudentUpdateRequestDto;
import com.smartinstitute.erp.academic.student.service.StudentService;
import com.smartinstitute.erp.academic.teacher.service.TeacherPermissionService;
import com.smartinstitute.erp.auth.entity.TenantContext;
import com.smartinstitute.erp.platform.institute.entity.Institute;
import com.smartinstitute.erp.platform.institute.repository.InstituteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/institute/students")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('INSTITUTE_ADMIN','PRINCIPAL','TEACHER')")
public class StudentController {

    private final StudentService service;
    private final InstituteRepository instituteRepository;
    private final TeacherPermissionService teacherPermissionService;

    private Institute currentInstitute(Long tenantId) {
        return instituteRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Institute not found"));
    }

    @PostMapping
    public StudentResponseDto create(@RequestBody StudentRequestDto dto) {
        return service.create(dto, currentInstitute(TenantContext.getTenantId()));
    }
    @PostMapping("/upload")
    public Map<String, String> uploadStudents(
            @RequestParam("classroomId") Long classroomId,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No file received. Send a multipart form with fields \"file\" (CSV or xlsx) and \"classroomId\" (number).");
        }
        service.importStudentsFromUpload(file, classroomId);
        return Map.of("message", "Students imported successfully");
    }

    @GetMapping
    public List<StudentResponseDto> list() {
        Institute inst = currentInstitute(TenantContext.getTenantId());
        if (teacherPermissionService.isTeacherRole()) {
            return service.listForTeacher(inst);
        }
        return service.list(inst);
    }

    @GetMapping("/{id}")
    public StudentResponseDto get(@PathVariable Long id) {
        return service.get(id, currentInstitute(TenantContext.getTenantId()));
    }

    @PutMapping("/{id}")
    public StudentResponseDto update(@PathVariable Long id, @RequestBody StudentUpdateRequestDto dto) {
        return service.update(id, dto, currentInstitute(TenantContext.getTenantId()));
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id, currentInstitute(TenantContext.getTenantId()));
        return "Student deleted successfully!!";
    }
}
