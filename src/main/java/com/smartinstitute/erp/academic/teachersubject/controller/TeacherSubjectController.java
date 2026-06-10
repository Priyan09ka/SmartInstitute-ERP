package com.smartinstitute.erp.academic.teachersubject.controller;

import com.smartinstitute.erp.academic.teachersubject.dto.TeacherSubjectRequestDto;
import com.smartinstitute.erp.academic.teachersubject.dto.TeacherSubjectResponseDto;
import com.smartinstitute.erp.academic.teachersubject.service.TeacherSubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher-subjects")
@RequiredArgsConstructor
public class TeacherSubjectController {

    private final TeacherSubjectService service;

    @PostMapping
    public TeacherSubjectResponseDto assign(
            @RequestBody TeacherSubjectRequestDto dto) {

        return service.assign(dto);
    }

    @GetMapping
    public List<TeacherSubjectResponseDto> list() {

        return service.list();
    }
    @PutMapping("/{id}")
    public TeacherSubjectResponseDto update(
            @PathVariable Long id,
            @RequestBody TeacherSubjectRequestDto dto) {

        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {

        service.delete(id);
    }
}