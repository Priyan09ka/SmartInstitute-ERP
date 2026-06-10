package com.smartinstitute.erp.academic.teacher.controller;

import com.smartinstitute.erp.academic.teacher.dto.TeacherRequestDto;
import com.smartinstitute.erp.academic.teacher.dto.TeacherResponseDto;
import com.smartinstitute.erp.academic.teacher.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('TEACHER')")
    public TeacherResponseDto me() {
        return teacherService.getMe();
    }

    @PostMapping
    public TeacherResponseDto create(@RequestBody TeacherRequestDto dto) {
        return teacherService.create(dto);
    }

    @GetMapping
    public List<TeacherResponseDto> list() {
        return teacherService.list();
    }
    /** Numeric id only — avoids `/me` being captured as a path variable (400 from conversion). */
    @GetMapping("/{id:[0-9]+}")
    public TeacherResponseDto get(@PathVariable Long id) {
        return teacherService.get(id);
    }

    @PutMapping("/{id:[0-9]+}")
    public TeacherResponseDto update(@PathVariable Long id,
                                     @RequestBody TeacherRequestDto dto) {
        return teacherService.update(id, dto);
    }

    @DeleteMapping("/{id:[0-9]+}")
    public String delete(@PathVariable Long id) {
        teacherService.delete(id);
        return "Teacher deleted successfully!!";
    }
}
