package com.smartinstitute.erp.academic.schedule.service;

import com.smartinstitute.erp.academic.classroom.entity.Classroom;
import com.smartinstitute.erp.academic.classroom.repository.ClassroomRepository;
import com.smartinstitute.erp.academic.schedule.dto.ScheduleSlotRequestDto;
import com.smartinstitute.erp.academic.schedule.dto.ScheduleSlotResponseDto;
import com.smartinstitute.erp.academic.schedule.entity.ScheduleSlot;
import com.smartinstitute.erp.academic.schedule.repository.ScheduleSlotRepository;
import com.smartinstitute.erp.academic.subject.entity.Subject;
import com.smartinstitute.erp.academic.subject.repository.SubjectRepository;
import com.smartinstitute.erp.academic.teacher.entity.Teacher;
import com.smartinstitute.erp.academic.teacher.repository.TeacherRepository;
import com.smartinstitute.erp.auth.entity.TenantContext;
import com.smartinstitute.erp.platform.institute.entity.Institute;
import com.smartinstitute.erp.platform.institute.repository.InstituteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleSlotService {

    private static final DateTimeFormatter[] TIME_FORMATS = new DateTimeFormatter[]{
            DateTimeFormatter.ISO_LOCAL_TIME,
            DateTimeFormatter.ofPattern("H:mm"),
            DateTimeFormatter.ofPattern("HH:mm")
    };

    private final ScheduleSlotRepository repo;
    private final InstituteRepository instituteRepository;
    private final ClassroomRepository classroomRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;

    @Transactional
    public ScheduleSlotResponseDto create(ScheduleSlotRequestDto dto) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new RuntimeException("Tenant not resolved from token");
        }

        Institute institute = instituteRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Institute not found"));

        Classroom classroom = classroomRepository.findById(dto.getClassroomId())
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        Teacher teacher = teacherRepository.findById(dto.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
        Subject subject = subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        String day = normalizeDay(dto.getDayOfWeek());
        LocalTime start = parseTime(dto.getStartTime());
        LocalTime end = parseTime(dto.getEndTime());
        if (!end.isAfter(start)) {
            throw new RuntimeException("End time must be after start time");
        }

        ScheduleSlot slot = new ScheduleSlot();
        slot.setInstitute(institute);
        slot.setClassroom(classroom);
        slot.setTeacher(teacher);
        slot.setSubject(subject);
        slot.setDayOfWeek(day);
        slot.setStartTime(start);
        slot.setEndTime(end);

        repo.save(slot);
        return toDto(slot);
    }

    @Transactional(readOnly = true)
    public List<ScheduleSlotResponseDto> list() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new RuntimeException("Tenant not resolved from token");
        }
        return repo.findByInstitute_Id(tenantId).stream().map(this::toDto).toList();
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getTenantId();
        ScheduleSlot slot = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule slot not found"));
        if (!slot.getInstitute().getId().equals(tenantId)) {
            throw new RuntimeException("Schedule slot not found");
        }
        repo.delete(slot);
    }

    private String normalizeDay(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new RuntimeException("Day of week is required");
        }
        String u = raw.trim().toUpperCase();
        if (u.length() >= 3) {
            u = u.substring(0, 3);
        }
        return switch (u) {
            case "MON" -> "MON";
            case "TUE" -> "TUE";
            case "WED" -> "WED";
            case "THU" -> "THU";
            case "FRI" -> "FRI";
            case "SAT" -> "SAT";
            case "SUN" -> "SUN";
            default -> throw new RuntimeException("Invalid day: use MON–SUN");
        };
    }

    private LocalTime parseTime(String s) {
        if (s == null || s.isBlank()) {
            throw new RuntimeException("Time is required");
        }
        String t = s.trim();
        for (DateTimeFormatter fmt : TIME_FORMATS) {
            try {
                return LocalTime.parse(t, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        try {
            return LocalTime.parse(t);
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Invalid time format (use HH:mm)");
        }
    }

    private ScheduleSlotResponseDto toDto(ScheduleSlot s) {
        ScheduleSlotResponseDto dto = new ScheduleSlotResponseDto();
        dto.setId(s.getId());
        dto.setClassroomId(s.getClassroom().getId());
        dto.setClassroomName(s.getClassroom().getName());
        dto.setTeacherId(s.getTeacher().getId());
        dto.setTeacherName(s.getTeacher().getUser().getName());
        dto.setSubjectId(s.getSubject().getId());
        dto.setSubjectName(s.getSubject().getName());
        dto.setDayOfWeek(s.getDayOfWeek());
        dto.setStartTime(s.getStartTime().toString());
        dto.setEndTime(s.getEndTime().toString());
        return dto;
    }
}
