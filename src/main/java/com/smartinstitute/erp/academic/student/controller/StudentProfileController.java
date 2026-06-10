package com.smartinstitute.erp.academic.student.controller;

import com.smartinstitute.erp.academic.student.dto.StudentMeDto;
import com.smartinstitute.erp.academic.student.dto.StudentNameUpdateRequestDto;
import com.smartinstitute.erp.academic.student.entity.Student;
import com.smartinstitute.erp.academic.student.service.StudentAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentProfileController {

    private final StudentAccountService studentAccountService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public StudentMeDto me() {
        Student s = studentAccountService.requireCurrentStudent();
        return toMeDto(s);
    }

    @PatchMapping("/me/name")
    @PreAuthorize("hasRole('STUDENT')")
    public StudentMeDto updateMyName(@RequestBody StudentNameUpdateRequestDto req) {
        Student s = studentAccountService.updateCurrentStudentName(req != null ? req.getName() : null);
        return toMeDto(s);
    }

    private StudentMeDto toMeDto(Student s) {
        StudentMeDto dto = new StudentMeDto();
        dto.setId(s.getId());
        dto.setUserId(s.getUser() != null ? s.getUser().getId() : 0L);
        dto.setName(s.getUser() != null ? s.getUser().getName() : null);
        dto.setEmail(s.getUser() != null ? s.getUser().getEmail() : null);
        dto.setRollNumber(s.getRollNumber());
        dto.setClassroomId(s.getClassroom() != null ? s.getClassroom().getId() : 0L);
        dto.setClassroomName(s.getClassroom() != null ? s.getClassroom().getName() : null);
        dto.setInstituteName(s.getInstitute() != null ? s.getInstitute().getName() : null);
        return dto;
    }
}
