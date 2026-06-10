package com.smartinstitute.erp.academic.attendance.service;
import com.smartinstitute.erp.academic.attendance.dto.AttendanceReportDto;
import com.smartinstitute.erp.academic.attendance.dto.AttendanceRequestDto;
import com.smartinstitute.erp.academic.attendance.dto.AttendanceResponseDto;
import com.smartinstitute.erp.academic.attendance.dto.AttendanceSummaryDto;
import com.smartinstitute.erp.academic.attendance.dto.BulkAttendanceRequestDto;
import com.smartinstitute.erp.academic.attendance.dto.StudentAttendanceDto;
import com.smartinstitute.erp.academic.attendance.entity.Attendance;
import com.smartinstitute.erp.academic.attendance.entity.AttendanceStatus;
import com.smartinstitute.erp.academic.attendance.repository.AttendanceRepository;
import com.smartinstitute.erp.academic.classroom.entity.Classroom;
import com.smartinstitute.erp.academic.classroom.repository.ClassroomRepository;
import com.smartinstitute.erp.academic.student.entity.Student;
import com.smartinstitute.erp.academic.student.repository.StudentRepository;
import com.smartinstitute.erp.academic.student.service.StudentAccountService;
import com.smartinstitute.erp.academic.teacher.service.TeacherPermissionService;
import com.smartinstitute.erp.auth.entity.TenantContext;
import com.smartinstitute.erp.notification.inapp.service.InAppNotificationService;
import com.smartinstitute.erp.platform.institute.entity.Institute;
import com.smartinstitute.erp.platform.institute.repository.InstituteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository repo;
    private final StudentRepository studentRepository;
    private final ClassroomRepository classroomRepository;
    private final InstituteRepository instituteRepository;
    private final TeacherPermissionService teacherPermissionService;
    private final StudentAccountService studentAccountService;
    private final InAppNotificationService inAppNotificationService;

    public AttendanceResponseDto markAttendance(AttendanceRequestDto dto){

        Long tenantId = TenantContext.getTenantId();

        teacherPermissionService.assertCanMarkAttendance(dto.getClassroomId());

        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Classroom classroom = classroomRepository.findById(dto.getClassroomId())
                .orElseThrow(() -> new RuntimeException("Classroom not found"));

        Institute institute = instituteRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Institute not found"));

        Attendance attendance = new Attendance();
        attendance.setStudent(student);
        attendance.setClassroom(classroom);
        attendance.setInstitute(institute);
        attendance.setDate(dto.getDate());
        attendance.setStatus(dto.getStatus());

        repo.save(attendance);

        inAppNotificationService.notifyUser(
                student.getUser(),
                String.format("Attendance for %s: %s in %s", dto.getDate(), dto.getStatus(), classroom.getName()));

        return toDto(attendance);
    }

    public List<AttendanceResponseDto> getClassAttendance(Long classroomId, java.time.LocalDate date){

        teacherPermissionService.assertCanMarkAttendance(classroomId);

        return repo.findByClassroom_IdAndDate(classroomId,date)
                .stream()
                .map(this::toDto)
                .toList();
    }
    public AttendanceResponseDto update(Long id, AttendanceRequestDto dto){

        Attendance attendance = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Attendance not found"));

        attendance.setStatus(dto.getStatus());

        repo.save(attendance);

        return toDto(attendance);
    }

    public void markBulkAttendance(BulkAttendanceRequestDto dto) {

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Institute context missing from your session. Log out and sign in again.");
        }
        if (dto.getClassroomId() == null || dto.getDate() == null || dto.getStudents() == null || dto.getStudents().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "classroomId, date, and a non-empty students list are required.");
        }

        teacherPermissionService.assertCanMarkAttendance(dto.getClassroomId());

        Classroom classroom = classroomRepository.findByIdAndInstitute_Id(dto.getClassroomId(), tenantId)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));

        Institute institute = instituteRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Institute not found"));

        for (StudentAttendanceDto studentDto : dto.getStudents()) {

            Student student = studentRepository
                    .findByIdAndInstitute_Id(studentDto.getStudentId(), tenantId)
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            Attendance attendance = repo
                    .findByStudent_IdAndDateAndInstitute_Id(student.getId(), dto.getDate(), tenantId)
                    .orElseGet(() -> {
                        Attendance a = new Attendance();
                        a.setStudent(student);
                        a.setClassroom(classroom);
                        a.setInstitute(institute);
                        a.setDate(dto.getDate());
                        return a;
                    });

            attendance.setStudent(student);
            attendance.setClassroom(classroom);
            attendance.setInstitute(institute);
            attendance.setDate(dto.getDate());
            attendance.setStatus(studentDto.getStatus());

            repo.save(attendance);

            String msg = String.format(
                    "Attendance for %s: %s in %s",
                    dto.getDate(),
                    studentDto.getStatus(),
                    classroom.getName());
            inAppNotificationService.notifyUser(student.getUser(), msg);
        }
    }

    /** Logged-in student: their attendance rows newest first. */
    public List<AttendanceResponseDto> listMyAttendance() {
        var student = studentAccountService.requireCurrentStudent();
        return repo.findByStudent_IdOrderByDateDesc(student.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    public AttendanceSummaryDto getSummaryForDate(java.time.LocalDate date) {

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new RuntimeException("Tenant not resolved from token");
        }

        long present = repo.countByInstitute_IdAndDateAndStatus(
                tenantId, date, AttendanceStatus.PRESENT);
        long absent = repo.countByInstitute_IdAndDateAndStatus(
                tenantId, date, AttendanceStatus.ABSENT);
        long holiday = repo.countByInstitute_IdAndDateAndStatus(
                tenantId, date, AttendanceStatus.HOLIDAY);

        AttendanceSummaryDto dto = new AttendanceSummaryDto();
        dto.setDate(date);
        dto.setPresentCount(present);
        dto.setAbsentCount(absent);
        dto.setHolidayCount(holiday);
        dto.setTotalMarked(present + absent + holiday);
        return dto;
    }

    public AttendanceReportDto getStudentAttendanceReport(Long studentId) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (studentAccountService.isStudentRole()) {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            if (!student.getUser().getEmail().equalsIgnoreCase(email)) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN,
                        "You can only view your own attendance report");
            }
        }

        long presentDays = repo.countByStudent_IdAndStatus(
                studentId, AttendanceStatus.PRESENT);

        long absentDays = repo.countByStudent_IdAndStatus(
                studentId, AttendanceStatus.ABSENT);

        long holidayDays = repo.countByStudent_IdAndStatus(
                studentId, AttendanceStatus.HOLIDAY);

        long totalClasses = presentDays + absentDays + holidayDays;

        double percentage = 0;
        long denom = presentDays + absentDays;
        if (denom > 0) {
            percentage = (presentDays * 100.0) / denom;
        }

        AttendanceReportDto dto = new AttendanceReportDto();

        dto.setStudentId(student.getId());
        dto.setStudentName(student.getUser().getName());

        dto.setTotalClasses(totalClasses);
        dto.setPresentDays(presentDays);
        dto.setAbsentDays(absentDays);
        dto.setHolidayDays(holidayDays);
        dto.setPercentage(percentage);

        return dto;
    }
    private AttendanceResponseDto toDto(Attendance a){

        AttendanceResponseDto dto = new AttendanceResponseDto();

        dto.setId(a.getId());
        dto.setStudentId(a.getStudent().getId());
        dto.setStudentName(a.getStudent().getUser().getName());

        dto.setClassroomId(a.getClassroom().getId());
        dto.setClassroomName(a.getClassroom().getName());

        dto.setDate(a.getDate());
        dto.setStatus(a.getStatus());

        return dto;
    }
}