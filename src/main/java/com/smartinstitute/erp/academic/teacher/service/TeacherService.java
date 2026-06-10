package com.smartinstitute.erp.academic.teacher.service;

import com.smartinstitute.erp.academic.classteacher.repository.ClassTeacherRepository;
import com.smartinstitute.erp.academic.teachersubject.repository.TeacherSubjectRepository;
import com.smartinstitute.erp.academic.teacher.dto.TeacherRequestDto;
import com.smartinstitute.erp.academic.teacher.dto.TeacherResponseDto;
import com.smartinstitute.erp.academic.teacher.entity.Teacher;
import com.smartinstitute.erp.academic.teacher.repository.TeacherRepository;
import com.smartinstitute.erp.auth.entity.TenantContext;
import com.smartinstitute.erp.platform.institute.entity.Institute;
import com.smartinstitute.erp.platform.institute.repository.InstituteRepository;
import com.smartinstitute.erp.user.entity.User;
import com.smartinstitute.erp.user.entity.UserInstitute;
import com.smartinstitute.erp.user.enums.Role;
import com.smartinstitute.erp.user.enums.Status;
import com.smartinstitute.erp.user.repository.UserInstituteRepository;
import com.smartinstitute.erp.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherService {
    private final TeacherPermissionService teacherPermissionService;
    private final ClassTeacherRepository classTeacherRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final InstituteRepository instituteRepository;
    private final UserInstituteRepository userInstituteRepository;
    private final PasswordEncoder passwordEncoder;

    public TeacherResponseDto create(TeacherRequestDto dto) {

        String pwd = dto.getPassword() != null ? dto.getPassword().trim() : "";
        String confirm = dto.getConfirmPassword() != null ? dto.getConfirmPassword().trim() : "";
        if (pwd.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }
        if (!pwd.equals(confirm)) {
            throw new RuntimeException("Password and confirm password do not match");
        }

        Long tenantId = TenantContext.getTenantId();

        Institute institute = instituteRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Institute not found"));
        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(pwd));
        user.setRole(Role.TEACHER);
        user.setStatus(Status.ACTIVE);

        userRepository.save(user);

        Teacher teacher = new Teacher();
        teacher.setUser(user);
        teacher.setInstitute(institute);

        teacherRepository.save(teacher);

        UserInstitute ui=new UserInstitute();
        ui.setUser(user);
        ui.setInstitute(institute);
        ui.setRole(Role.TEACHER);
        userInstituteRepository.save(ui);

        return toDto(teacher);
    }

    /** Current logged-in teacher (ROLE_TEACHER), with homeroom / subject flags for the UI. */
    public TeacherResponseDto getMe() {
        Teacher t = teacherPermissionService.getCurrentTeacher();
        Long tenantId = TenantContext.getTenantId();
        TeacherResponseDto dto = toDto(t);
        if (tenantId != null) {
            List<Long> homeroom = classTeacherRepository.findByTeacher_Id(t.getId()).stream()
                    .filter(ct -> ct.getInstitute().getId().equals(tenantId))
                    .map(ct -> ct.getClassroom().getId())
                    .toList();
            dto.setHomeroomClassroomIds(homeroom);
            boolean subject = teacherSubjectRepository.findByTeacher_Id(t.getId()).stream()
                    .anyMatch(ts -> ts.getInstitute().getId().equals(tenantId));
            dto.setAssignedAsSubjectTeacher(subject);
        }
        return dto;
    }

    public List<TeacherResponseDto> list() {

        Long tenantId = TenantContext.getTenantId();

        return teacherRepository.findByInstitute_Id(tenantId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public TeacherResponseDto get(Long id) {

        Long tenantId = TenantContext.getTenantId();

        Teacher teacher = teacherRepository
                .findByIdAndInstitute_Id(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Teacher not found"));

        return toDto(teacher);
    }

    public TeacherResponseDto update(Long id, TeacherRequestDto dto) {

        Long tenantId = TenantContext.getTenantId();

        Teacher teacher = teacherRepository
                .findByIdAndInstitute_Id(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Teacher not found"));

        User user = teacher.getUser();

        user.setName(dto.getName());
        user.setEmail(dto.getEmail());

        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        userRepository.save(user);

        return toDto(teacher);
    }

    public void delete(Long id) {

        Long tenantId = TenantContext.getTenantId();

        Teacher teacher = teacherRepository
                .findByIdAndInstitute_Id(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Teacher not found"));

        userRepository.delete(teacher.getUser());
        teacherRepository.delete(teacher);
    }

    private TeacherResponseDto toDto(Teacher t) {

        TeacherResponseDto dto = new TeacherResponseDto();

        dto.setId(t.getId());
        dto.setUserId(t.getUser().getId());
        dto.setName(t.getUser().getName());
        dto.setEmail(t.getUser().getEmail());
        dto.setRole(t.getUser().getRole().name());
        dto.setStatus(t.getUser().getStatus().name());
        dto.setInstituteName(t.getInstitute().getName());

        return dto;
    }

}
