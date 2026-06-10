package com.smartinstitute.erp.academic.teachersubject.service;

import com.smartinstitute.erp.academic.subject.entity.Subject;
import com.smartinstitute.erp.academic.subject.repository.SubjectRepository;
import com.smartinstitute.erp.academic.teacher.entity.Teacher;
import com.smartinstitute.erp.academic.teacher.repository.TeacherRepository;
import com.smartinstitute.erp.academic.teachersubject.dto.TeacherSubjectRequestDto;
import com.smartinstitute.erp.academic.teachersubject.dto.TeacherSubjectResponseDto;
import com.smartinstitute.erp.academic.teachersubject.entity.TeacherSubject;
import com.smartinstitute.erp.academic.teachersubject.repository.TeacherSubjectRepository;
import com.smartinstitute.erp.auth.entity.TenantContext;
import com.smartinstitute.erp.platform.institute.entity.Institute;
import com.smartinstitute.erp.platform.institute.repository.InstituteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherSubjectService {

    private final TeacherSubjectRepository repo;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final InstituteRepository instituteRepository;

    // Assign subject to teacher
    public TeacherSubjectResponseDto assign(TeacherSubjectRequestDto dto) {

        Long tenantId = TenantContext.getTenantId();

        Teacher teacher = teacherRepository.findById(dto.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        Subject subject = subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        Institute institute = instituteRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Institute not found"));

        TeacherSubject ts = new TeacherSubject();
        ts.setTeacher(teacher);
        ts.setSubject(subject);
        ts.setInstitute(institute);

        repo.save(ts);

        return toDto(ts);
    }

    // List all assignments
    public List<TeacherSubjectResponseDto> list() {

        Long tenantId = TenantContext.getTenantId();

        return repo.findByInstitute_Id(tenantId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public TeacherSubjectResponseDto update(Long id, TeacherSubjectRequestDto dto) {

        TeacherSubject ts = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        Teacher teacher = teacherRepository.findById(dto.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        Subject subject = subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        ts.setTeacher(teacher);
        ts.setSubject(subject);

        repo.save(ts);

        return toDto(ts);
    }

    // Delete assignment
    public void delete(Long id) {

        TeacherSubject ts = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Assignment not found"));

        repo.delete(ts);
    }

    private TeacherSubjectResponseDto toDto(TeacherSubject ts) {

        TeacherSubjectResponseDto dto = new TeacherSubjectResponseDto();

        dto.setId(ts.getId());
        dto.setTeacherId(ts.getTeacher().getId());
        dto.setTeacherName(ts.getTeacher().getUser().getName());

        dto.setSubjectId(ts.getSubject().getId());
        dto.setSubjectName(ts.getSubject().getName());

        return dto;
    }
}