package com.smartinstitute.erp.academic.student.service;

import com.smartinstitute.erp.academic.classroom.entity.Classroom;
import com.smartinstitute.erp.academic.classroom.repository.ClassroomRepository;

import com.smartinstitute.erp.academic.student.dto.StudentRequestDto;
import com.smartinstitute.erp.academic.student.dto.StudentResponseDto;
import com.smartinstitute.erp.academic.student.dto.StudentUpdateRequestDto;
import com.smartinstitute.erp.academic.student.entity.Student;
import com.smartinstitute.erp.academic.student.repository.StudentRepository;
import com.smartinstitute.erp.academic.teacher.service.TeacherPermissionService;

import com.smartinstitute.erp.common.util.PasswordGenerator;
import com.smartinstitute.erp.notification.mail.MailService;
import com.smartinstitute.erp.platform.institute.entity.Institute;
import com.smartinstitute.erp.user.entity.User;
import com.smartinstitute.erp.user.entity.UserInstitute;
import com.smartinstitute.erp.user.enums.Role;
import com.smartinstitute.erp.user.enums.Status;
import com.smartinstitute.erp.user.repository.UserInstituteRepository;
import com.smartinstitute.erp.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ClassroomRepository classroomRepository;
    private final UserInstituteRepository userInstituteRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final TeacherPermissionService teacherPermissionService;

    private static final DataFormatter CELL_FORMATTER = new DataFormatter();


    @Transactional
    public void importStudentsFromUpload(MultipartFile file, Long classroomId) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is required");
        }
        String fn = file.getOriginalFilename();
        String ct = file.getContentType();
        boolean csv = fn != null && fn.toLowerCase(Locale.ROOT).endsWith(".csv")
                || ct != null && ct.toLowerCase(Locale.ROOT).contains("csv");
        if (csv) {
            importStudentsFromCsv(file, classroomId);
        } else {
            importStudentsFromExcel(file, classroomId);
        }
    }

    @Transactional
    public void importStudentsFromCsv(MultipartFile file, Long classroomId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));
        teacherPermissionService.assertClassTeacherForClassroom(classroomId);

        try (var reader = new InputStreamReader(openCsvStreamSkippingBom(file.getInputStream()), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .setIgnoreEmptyLines(true)
                     .build()
                     .parse(reader)) {

            int imported = 0;
            for (CSVRecord record : parser) {
                Map<String, String> norm = normalizedRow(record);
                String name = first(norm, "name", "studentname", "fullname", "student");
                String email = first(norm, "email", "mail", "e-mail");
                String roll = first(norm, "rollnumber", "roll", "rollno", "roll_no", "id");
                if (name.isEmpty() && email.isEmpty()) {
                    continue;
                }
                if (name.isEmpty() || email.isEmpty()) {
                    throw new RuntimeException("Row " + record.getRecordNumber() + ": name and email are required");
                }
                if (userRepository.existsByEmail(email.trim())) {
                    continue;
                }
                persistImportedStudent(name, email, roll.isEmpty() ? "-" : roll, classroom);
                imported++;
            }
            if (imported == 0) {
                throw new RuntimeException(
                        "No new students imported. Either the file only had headers, all emails already exist, or rows were empty. Check CSV columns: name, email, rollNumber.");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to import students from CSV", e);
        }
    }

    @Transactional
    public void importStudentsFromExcel(MultipartFile file, Long classroomId) {
        try {
            InputStream is = file.getInputStream();
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);

            Classroom classroom = classroomRepository.findById(classroomId)
                    .orElseThrow(() -> new RuntimeException("Classroom not found"));

            teacherPermissionService.assertClassTeacherForClassroom(classroomId);

            int imported = 0;
            for (Row row : sheet) {
                if (row.getRowNum() == 0) {
                    continue;
                }
                String name = cellString(row, 0);
                String email = cellString(row, 1);
                String rollNumber = cellString(row, 2);
                if (name.isEmpty() && email.isEmpty()) {
                    continue;
                }
                if (name.isEmpty() || email.isEmpty()) {
                    throw new RuntimeException("Excel row " + (row.getRowNum() + 1) + ": name and email are required");
                }
                if (userRepository.existsByEmail(email.trim())) {
                    continue;
                }
                persistImportedStudent(name, email, rollNumber.isEmpty() ? "-" : rollNumber, classroom);
                imported++;
            }
            if (imported == 0) {
                throw new RuntimeException(
                        "No new students imported. All emails may already exist, or the sheet had no data rows.");
            }

            workbook.close();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to import students from Excel", e);
        }
    }

    private void persistImportedStudent(String name, String email, String rollNumber, Classroom classroom) {
        String em = email.trim();
        String plainPassword = PasswordGenerator.generatePassword(8);

        User user = new User();
        user.setName(name.trim());
        user.setEmail(em);
        user.setPassword(passwordEncoder.encode(plainPassword));
        user.setRole(Role.STUDENT);
        user.setStatus(Status.ACTIVE);
        user.setFirstLogin(true);

        userRepository.save(user);

        Student student = new Student();
        student.setUser(user);
        student.setRollNumber(rollNumber.trim());
        student.setClassroom(classroom);
        student.setInstitute(classroom.getInstitute());

        studentRepository.save(student);
        UserInstitute ui = new UserInstitute();
        ui.setUser(user);
        ui.setInstitute(classroom.getInstitute());
        ui.setRole(Role.STUDENT);

        userInstituteRepository.save(ui);

        sendCredentialsSafely(em, name.trim(), plainPassword, classroom.getInstitute().getName());
    }

    /**
     * Never fail student save/import if SMTP misconfigured — Angus/JavaMail can throw errors that
     * surface as {@code RuntimeException} with message "Authentication failed".
     */
    private void sendCredentialsSafely(String email, String studentName, String plainPassword, String instituteName) {
        try {
            mailService.sendStudentCredentials(email, studentName, plainPassword, instituteName);
        } catch (Throwable t) {
            log.warn("Could not email login credentials to {} (student was still saved): {}", email, t.getMessage());
        }
    }

    /** Skip UTF-8 BOM so header "name" is recognized (Excel-exported CSVs often include BOM). */
    private static InputStream openCsvStreamSkippingBom(InputStream in) throws java.io.IOException {
        PushbackInputStream pin = new PushbackInputStream(in, 3);
        byte[] probe = new byte[3];
        int n = pin.read(probe);
        if (n == 3 && probe[0] == (byte) 0xEF && probe[1] == (byte) 0xBB && probe[2] == (byte) 0xBF) {
            return pin;
        }
        if (n > 0) {
            pin.unread(probe, 0, n);
        }
        return pin;
    }

    private static String cellString(Row row, int col) {
        Cell c = row.getCell(col);
        if (c == null || c.getCellType() == CellType.BLANK) {
            return "";
        }
        return CELL_FORMATTER.formatCellValue(c).trim();
    }

    private static Map<String, String> normalizedRow(CSVRecord record) {
        Map<String, String> norm = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : record.toMap().entrySet()) {
            if (e.getKey() == null) {
                continue;
            }
            String k = e.getKey().toLowerCase(Locale.ROOT).replaceAll("[\\s_-]+", "");
            norm.put(k, e.getValue() != null ? e.getValue().trim() : "");
        }
        return norm;
    }

    private static String first(Map<String, String> norm, String... keys) {
        for (String k : keys) {
            String v = norm.get(k);
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    /**
     * Create Student
     */
    @Transactional
    public StudentResponseDto create(StudentRequestDto dto, Institute institute) {

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        teacherPermissionService.assertClassTeacherForClassroom(dto.getClassroomId());

        String plainPassword = PasswordGenerator.generatePassword(8);

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(plainPassword));
        user.setRole(Role.STUDENT);
        user.setStatus(Status.ACTIVE);
        user.setFirstLogin(true);

        userRepository.save(user);

        Classroom classroom = classroomRepository.findById(dto.getClassroomId())
                .orElseThrow(() -> new RuntimeException("Classroom not found"));

        Student student = new Student();
        student.setUser(user);
        student.setRollNumber(dto.getRollNumber());
        student.setClassroom(classroom);
        student.setInstitute(institute);

        studentRepository.save(student);
        UserInstitute ui = new UserInstitute();
        ui.setUser(user);
        ui.setInstitute(institute);
        ui.setRole(Role.STUDENT);

        userInstituteRepository.save(ui);

        sendCredentialsSafely(dto.getEmail(), dto.getName(), plainPassword, institute.getName());

        return toResponse(student);
    }

    /**
     * List students
     */
    public List<StudentResponseDto> list(Institute institute) {

        return studentRepository.findAllByInstitute_Id(institute.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Teachers only see students in classes they teach (homeroom or on schedule).
     */
    public List<StudentResponseDto> listForTeacher(Institute institute) {
        var teacher = teacherPermissionService.getCurrentTeacher();
        Set<Long> ids = teacherPermissionService.getAllowedClassroomIds(teacher, institute.getId());
        if (ids.isEmpty()) {
            return List.of();
        }
        return studentRepository.findByInstitute_IdAndClassroom_IdIn(institute.getId(), new ArrayList<>(ids))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Get student
     */
    public StudentResponseDto get(Long id, Institute institute) {

        Student student = studentRepository.findByIdAndInstitute_Id(id, institute.getId())
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));

        teacherPermissionService.assertTeacherCanAccessClassroom(student.getClassroom().getId());

        return toResponse(student);
    }

    /**
     * Update student
     */
    @Transactional
    public StudentResponseDto update(Long id, StudentUpdateRequestDto dto, Institute institute) {

        Student student = studentRepository.findByIdAndInstitute_Id(id, institute.getId())
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));

        teacherPermissionService.assertClassTeacherForClassroom(student.getClassroom().getId());

        student.getUser().setName(dto.getName());
        student.setRollNumber(dto.getRollNumber());

        Classroom classroom = classroomRepository.findById(dto.getClassroomId())
                .orElseThrow(() -> new RuntimeException("Classroom not found"));

        teacherPermissionService.assertClassTeacherForClassroom(dto.getClassroomId());

        student.setClassroom(classroom);

        studentRepository.save(student);

        return toResponse(student);
    }
    /**
     * Delete student
     */
    @Transactional
    public void delete(Long id, Institute institute) {

        Student student = studentRepository.findByIdAndInstitute_Id(id, institute.getId())
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));

        teacherPermissionService.assertClassTeacherForClassroom(student.getClassroom().getId());

        studentRepository.delete(student);
    }

    /**
     * Convert entity to response DTO
     */
    private StudentResponseDto toResponse(Student student) {

        return new StudentResponseDto(
                student.getId(),
                student.getClassroom().getId(),
                student.getUser().getId(),
                student.getUser().getName(),
                student.getUser().getEmail(),
                student.getRollNumber(),
                student.getClassroom().getName()
        );
    }
}
