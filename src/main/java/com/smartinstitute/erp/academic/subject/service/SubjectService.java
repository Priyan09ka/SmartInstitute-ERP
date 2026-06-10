package com.smartinstitute.erp.academic.subject.service;

import com.smartinstitute.erp.academic.course.entity.Course;
import com.smartinstitute.erp.academic.course.repository.CourseRepository;
import com.smartinstitute.erp.academic.schedule.entity.ScheduleSlot;
import com.smartinstitute.erp.academic.schedule.repository.ScheduleSlotRepository;
import com.smartinstitute.erp.academic.subject.dto.SubjectRequestDto;
import com.smartinstitute.erp.academic.subject.dto.SubjectResponseDto;
import com.smartinstitute.erp.academic.subject.entity.Subject;
import com.smartinstitute.erp.academic.subject.repository.SubjectRepository;
import com.smartinstitute.erp.academic.teacher.service.TeacherPermissionService;
import com.smartinstitute.erp.auth.entity.TenantContext;
import com.smartinstitute.erp.platform.institute.entity.Institute;
import com.smartinstitute.erp.platform.institute.repository.InstituteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;
    private final InstituteRepository instituteRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final TeacherPermissionService teacherPermissionService;

    public SubjectResponseDto create(SubjectRequestDto dto) {

        Long tenantId = TenantContext.getTenantId();

        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Institute institute = instituteRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Institute not found"));

        if (subjectRepository.existsByNameAndCourse_IdAndInstitute_Id(
                dto.getName(), dto.getCourseId(), tenantId)) {

            throw new RuntimeException("Subject already exists");
        }

        Subject subject = new Subject();
        subject.setName(dto.getName());
        subject.setCode(dto.getCode());
        subject.setCourse(course);
        subject.setInstitute(institute);

        subjectRepository.save(subject);

        return toDto(subject);
    }

    public List<SubjectResponseDto> list() {

        Long tenantId = TenantContext.getTenantId();

        return subjectRepository.findByInstitute_Id(tenantId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Subjects taught in this class per the timetable (distinct subjects from schedule slots).
     */
    public List<SubjectResponseDto> listForClassroom(Long classroomId) {
        Long tenantId = TenantContext.getTenantId();
        if (teacherPermissionService.isTeacherRole()) {
            teacherPermissionService.assertTeacherCanAccessClassroom(classroomId);
        }
        List<ScheduleSlot> slots = scheduleSlotRepository.findByInstitute_IdAndClassroom_Id(tenantId, classroomId);
        LinkedHashMap<Long, Subject> unique = new LinkedHashMap<>();
        for (ScheduleSlot slot : slots) {
            Subject sub = slot.getSubject();
            if (sub != null) {
                unique.putIfAbsent(sub.getId(), sub);
            }
        }
        return unique.values().stream().map(this::toDto).toList();
    }

    public SubjectResponseDto get(Long id) {

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subject not found"));

        return toDto(subject);
    }

    public SubjectResponseDto update(Long id, SubjectRequestDto dto) {

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subject not found"));

        subject.setName(dto.getName());
        subject.setCode(dto.getCode());

        subjectRepository.save(subject);

        return toDto(subject);
    }

    public void delete(Long id) {
        subjectRepository.deleteById(id);
    }

    private SubjectResponseDto toDto(Subject subject) {

        SubjectResponseDto dto = new SubjectResponseDto();

        dto.setId(subject.getId());
        dto.setName(subject.getName());
        dto.setCode(subject.getCode());
        dto.setCourseName(subject.getCourse().getName());

        return dto;
    }
}