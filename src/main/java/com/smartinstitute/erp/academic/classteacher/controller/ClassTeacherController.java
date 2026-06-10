package com.smartinstitute.erp.academic.classteacher.controller;

import com.smartinstitute.erp.academic.classteacher.dto.ClassTeacherRequestDto;
import com.smartinstitute.erp.academic.classteacher.dto.ClassTeacherResponseDto;
import com.smartinstitute.erp.academic.classteacher.service.ClassTeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/class-teachers")
@RequiredArgsConstructor
public class ClassTeacherController {

    private final ClassTeacherService service;

    @PostMapping
    public ClassTeacherResponseDto assign(@RequestBody ClassTeacherRequestDto dto) {
        return service.assign(dto);
    }

    @GetMapping
    public List<ClassTeacherResponseDto> list() {
        return service.list();
    }

    @PutMapping("/{id}")
    public ClassTeacherResponseDto update(
            @PathVariable Long id,
            @RequestBody ClassTeacherRequestDto dto) {

        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}