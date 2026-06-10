package com.smartinstitute.erp.academic.classroom.service;

import com.smartinstitute.erp.academic.classroom.dto.ClassroomRequestDto;
import com.smartinstitute.erp.academic.classroom.dto.ClassroomResponseDto;
import com.smartinstitute.erp.academic.classroom.entity.Classroom;
import com.smartinstitute.erp.academic.classroom.repository.ClassroomRepository;
import com.smartinstitute.erp.academic.teacher.service.TeacherPermissionService;
import com.smartinstitute.erp.auth.entity.TenantContext;
import com.smartinstitute.erp.platform.institute.entity.Institute;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomService {

    private final ClassroomRepository repo;
    private final TeacherPermissionService teacherPermissionService;

    public ClassroomResponseDto create(ClassroomRequestDto dto, Institute institute) {

        Long tenantId = TenantContext.getTenantId();

        if (repo.existsByNameAndInstitute_Id(dto.getName(), tenantId)) {
            throw new RuntimeException("Class already exists");
        }

        Classroom c = new Classroom();
        c.setName(dto.getName());

        Institute institute1=new Institute();
        institute1.setId(tenantId);
        c.setInstitute(institute);

        c = repo.save(c);
        return new ClassroomResponseDto(c.getId(), c.getName());
    }

    public List<ClassroomResponseDto> list(Institute institute) {
        return repo.findAllByInstitute_Id(institute.getId())
                .stream()
                .map(c -> new ClassroomResponseDto(c.getId(), c.getName()))
                .toList();
    }

    public List<ClassroomResponseDto> listForTeacher(Institute institute) {
        var teacher = teacherPermissionService.getCurrentTeacher();
        Set<Long> ids = teacherPermissionService.getAllowedClassroomIds(teacher, institute.getId());
        if (ids.isEmpty()) {
            return List.of();
        }
        return repo.findByInstitute_IdAndIdIn(institute.getId(), new ArrayList<>(ids))
                .stream()
                .map(c -> new ClassroomResponseDto(c.getId(), c.getName()))
                .toList();
    }

    public ClassroomResponseDto get(Long id, Institute institute) {
        Classroom c = repo.findByIdAndInstitute_Id(id, institute.getId())
                .orElseThrow(() -> new EntityNotFoundException("Class not found"));
        return new ClassroomResponseDto(c.getId(), c.getName());
    }

    public ClassroomResponseDto update(Long id, ClassroomRequestDto dto, Institute institute) {
        Classroom c = repo.findByIdAndInstitute_Id(id, institute.getId())
                .orElseThrow(() -> new EntityNotFoundException("Class not found"));
        if(repo.existsByNameAndInstitute_Id(dto.getName(), institute.getId())){
            throw new RuntimeException("class already exists");
        }

        c.setName(dto.getName());
        repo.save(c);

        return new ClassroomResponseDto(c.getId(), c.getName());
    }

    public String delete(Long id, Institute institute) {
        Classroom c = repo.findByIdAndInstitute_Id(id, institute.getId())
                .orElseThrow(() -> new EntityNotFoundException("Class not found"));
        repo.delete(c);
        return "Class deleted successfully!!";
    }
}

