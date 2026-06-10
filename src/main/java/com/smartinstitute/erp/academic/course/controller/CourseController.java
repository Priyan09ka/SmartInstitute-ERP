package com.smartinstitute.erp.academic.course.controller;

import com.smartinstitute.erp.academic.course.dto.CourseDto;
import com.smartinstitute.erp.academic.course.service.CourseService;
import com.smartinstitute.erp.platform.institute.entity.Institute;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTITUTE_ADMIN','PRINCIPAL')")
    public CourseDto create(@RequestBody CourseDto dto) {
        return courseService.create(dto);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('INSTITUTE_ADMIN','PRINCIPAL','TEACHER')")
    public List<CourseDto> list() {
        return courseService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTITUTE_ADMIN','PRINCIPAL','TEACHER')")
    public CourseDto get(@PathVariable Long id) {
        return courseService.get(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTITUTE_ADMIN','PRINCIPAL')")
    public CourseDto update(@PathVariable Long id, @RequestBody CourseDto dto) {
        return courseService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTITUTE_ADMIN','PRINCIPAL')")
    public String delete(@PathVariable Long id) {
        courseService.delete(id);
        return "Course deleted successfully!!";
    }
}