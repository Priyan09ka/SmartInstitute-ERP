package com.smartinstitute.erp.academic.classteacher.service;

import com.smartinstitute.erp.academic.classroom.entity.Classroom;
import com.smartinstitute.erp.academic.classroom.repository.ClassroomRepository;
import com.smartinstitute.erp.academic.classteacher.dto.ClassTeacherRequestDto;
import com.smartinstitute.erp.academic.classteacher.dto.ClassTeacherResponseDto;
import com.smartinstitute.erp.academic.classteacher.entity.ClassTeacher;
import com.smartinstitute.erp.academic.classteacher.repository.ClassTeacherRepository;
import com.smartinstitute.erp.academic.teacher.entity.Teacher;
import com.smartinstitute.erp.academic.teacher.repository.TeacherRepository;
import com.smartinstitute.erp.auth.entity.TenantContext;
import com.smartinstitute.erp.platform.institute.entity.Institute;
import com.smartinstitute.erp.platform.institute.repository.InstituteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassTeacherService {

    private final ClassTeacherRepository repo;
    private final ClassroomRepository classroomRepository;
    private final TeacherRepository teacherRepository;
    private final InstituteRepository instituteRepository;

    public ClassTeacherResponseDto assign(ClassTeacherRequestDto dto) {

        Long tenantId = TenantContext.getTenantId();

        Classroom classroom = classroomRepository.findById(dto.getClassroomId())
                .orElseThrow(() -> new RuntimeException("Classroom not found"));

        Teacher teacher = teacherRepository.findById(dto.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        Institute institute = instituteRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Institute not found"));

        ClassTeacher ct = new ClassTeacher();
        ct.setClassroom(classroom);
        ct.setTeacher(teacher);
        ct.setInstitute(institute);

        repo.save(ct);

        return toDto(ct);
    }

    public List<ClassTeacherResponseDto> list() {

        Long tenantId = TenantContext.getTenantId();

        return repo.findByInstitute_Id(tenantId)
                .stream()
                .map(this::toDto)
                .toList();
    }
    public ClassTeacherResponseDto update(Long id, ClassTeacherRequestDto dto) {

        ClassTeacher ct = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        Teacher teacher = teacherRepository.findById(dto.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        Classroom classroom = classroomRepository.findById(dto.getClassroomId())
                .orElseThrow(() -> new RuntimeException("Classroom not found"));

        ct.setTeacher(teacher);
        ct.setClassroom(classroom);

        repo.save(ct);

        return toDto(ct);
    }
    public void delete(Long id) {

        ClassTeacher ct = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        repo.delete(ct);
    }

    private ClassTeacherResponseDto toDto(ClassTeacher ct) {

        ClassTeacherResponseDto dto = new ClassTeacherResponseDto();

        dto.setId(ct.getId());
        dto.setClassroomId(ct.getClassroom().getId());
        dto.setClassroomName(ct.getClassroom().getName());

        dto.setTeacherId(ct.getTeacher().getId());
        dto.setTeacherName(ct.getTeacher().getUser().getName());

        return dto;
    }
}