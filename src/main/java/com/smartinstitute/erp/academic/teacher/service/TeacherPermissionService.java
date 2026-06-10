    package com.smartinstitute.erp.academic.teacher.service;

    import com.smartinstitute.erp.academic.classteacher.entity.ClassTeacher;
    import com.smartinstitute.erp.academic.classteacher.repository.ClassTeacherRepository;
    import com.smartinstitute.erp.academic.schedule.entity.ScheduleSlot;
    import com.smartinstitute.erp.academic.schedule.repository.ScheduleSlotRepository;
    import com.smartinstitute.erp.academic.teacher.entity.Teacher;
    import com.smartinstitute.erp.academic.teacher.repository.TeacherRepository;
    import com.smartinstitute.erp.academic.teachersubject.repository.TeacherSubjectRepository;
    import com.smartinstitute.erp.auth.entity.TenantContext;
    import com.smartinstitute.erp.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

    import java.util.LinkedHashSet;
    import java.util.Set;

    @Service
    @RequiredArgsConstructor
    public class TeacherPermissionService {

        private final TeacherRepository teacherRepository;
        private final ClassTeacherRepository classTeacherRepository;
        private final ScheduleSlotRepository scheduleSlotRepository;
        private final TeacherSubjectRepository teacherSubjectRepository;

        public boolean isTeacherRole() {
            return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                    .anyMatch(a -> "ROLE_TEACHER".equals(a.getAuthority()));
        }

        public Teacher getCurrentTeacher() {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            return teacherRepository.findByUser_Email(email)
                    .orElseThrow(() -> new UserNotFoundException(
                            "No teacher profile for this account. Ask your institute admin to register you as a teacher."));
        }

        /**
         * Classrooms where this teacher is homeroom teacher or appears on the schedule for the institute.
         */
        public Set<Long> getAllowedClassroomIds(Teacher teacher, Long instituteId) {
            Set<Long> ids = new LinkedHashSet<>();
            for (ClassTeacher ct : classTeacherRepository.findByTeacher_Id(teacher.getId())) {
                if (ct.getInstitute().getId().equals(instituteId)) {
                    ids.add(ct.getClassroom().getId());
                }
            }
            for (ScheduleSlot s : scheduleSlotRepository.findByInstitute_Id(instituteId)) {
                if (s.getTeacher().getId() == teacher.getId()) {
                    ids.add(s.getClassroom().getId());
                }
            }
            return ids;
        }

        /** Only the assigned class teacher may add/import students for that class. */
        public void assertClassTeacherForClassroom(Long classroomId) {
            if (!isTeacherRole()) {
                return;
            }
            Teacher t = getCurrentTeacher();
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Institute is missing from your session. Log out and sign in again.");
            }
            if (!classTeacherRepository.existsByTeacher_IdAndClassroom_IdAndInstitute_Id(
                    t.getId(), classroomId, tenantId)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Only the class teacher can add or import students for this class. In Principal → Class teachers, assign you as class teacher for this classroom, then try again.");
            }
        }

        /**
         * Same rule as {@link #getAllowedClassroomIds(Teacher, Long)}: if the class appears under
         * {@code /institute/classes} for this teacher, they may mark attendance for any calendar date
         * (including weekends — there is no day-of-week restriction).
         */
        public void assertCanMarkAttendance(Long classroomId) {
            if (!isTeacherRole()) {
                return;
            }
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                throw new RuntimeException("Tenant not resolved from token");
            }
            Teacher t = getCurrentTeacher();
            if (!getAllowedClassroomIds(t, tenantId).contains(classroomId)) {
                throw new RuntimeException("You are not assigned to take attendance for this class");
            }
        }

        /** Class teacher for the class, or assigned to teach the subject (teacher–subject). */
        public void assertCanManageQuiz(Long subjectId, Long classroomId) {
            if (!isTeacherRole()) {
                return;
            }
            Teacher t = getCurrentTeacher();
            boolean homeroom = classTeacherRepository.findByTeacher_IdAndClassroom_Id(t.getId(), classroomId).isPresent();
            boolean subjectTeacher = teacherSubjectRepository.existsByTeacher_IdAndSubject_Id(t.getId(), subjectId);
            if (!homeroom && !subjectTeacher) {
                throw new RuntimeException("You cannot manage a quiz for this subject/class");
            }
        }

        /** Homeroom or schedule: teacher may view students / attendance for this class. */
        public void assertTeacherCanAccessClassroom(Long classroomId) {
            if (!isTeacherRole()) {
                return;
            }
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                throw new RuntimeException("Tenant not resolved from token");
            }
            Teacher t = getCurrentTeacher();
            if (!getAllowedClassroomIds(t, tenantId).contains(classroomId)) {
                throw new RuntimeException("You are not assigned to this class");
            }
        }

        /** Used to filter quiz lists: must be in an allowed class and (homeroom or subject teacher). */
        public boolean canTeacherAccessQuiz(Teacher teacher, Long instituteId, Long subjectId, Long classroomId) {
            if (!getAllowedClassroomIds(teacher, instituteId).contains(classroomId)) {
                return false;
            }
            boolean homeroom = classTeacherRepository.findByTeacher_IdAndClassroom_Id(teacher.getId(), classroomId).isPresent();
            boolean subjectOk = teacherSubjectRepository.existsByTeacher_IdAndSubject_Id(teacher.getId(), subjectId);
            return homeroom || subjectOk;
        }
    }
