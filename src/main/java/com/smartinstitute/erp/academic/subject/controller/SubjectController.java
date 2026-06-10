package com.smartinstitute.erp.academic.subject.controller;

import com.smartinstitute.erp.academic.subject.dto.SubjectRequestDto;
import com.smartinstitute.erp.academic.subject.dto.SubjectResponseDto;
import com.smartinstitute.erp.academic.subject.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @PostMapping
    public SubjectResponseDto create(@RequestBody SubjectRequestDto dto) {
        return subjectService.create(dto);
    }

    @GetMapping
    public List<SubjectResponseDto> list(@RequestParam(required = false) Long classroomId) {
        if (classroomId != null) {
            return subjectService.listForClassroom(classroomId);
        }
        return subjectService.list();
    }

    @GetMapping("/{id}")
    public SubjectResponseDto get(@PathVariable Long id) {
        return subjectService.get(id);
    }

    @PutMapping("/{id}")
    public SubjectResponseDto update(@PathVariable Long id,
                                     @RequestBody SubjectRequestDto dto) {
        return subjectService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        subjectService.delete(id);
    }
}
