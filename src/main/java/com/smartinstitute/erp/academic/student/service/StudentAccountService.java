package com.smartinstitute.erp.academic.student.service;

import com.smartinstitute.erp.academic.student.entity.Student;
import com.smartinstitute.erp.academic.student.repository.StudentRepository;
import com.smartinstitute.erp.exception.UserNotFoundException;
import com.smartinstitute.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentAccountService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public boolean isStudentRole() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> "ROLE_STUDENT".equals(a.getAuthority()));
    }

    public Student requireCurrentStudent() {
        if (!isStudentRole()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student access only");
        }
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return studentRepository.findByUser_Email(email)
                .orElseThrow(() -> new UserNotFoundException(
                        "No student profile for this account. Ask your institute admin."));
    }

    public Optional<Student> currentStudentIfApplicable() {
        if (!isStudentRole()) {
            return Optional.empty();
        }
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return studentRepository.findByUser_Email(email);
    }

    @Transactional
    public Student updateCurrentStudentName(String name) {
        String next = name == null ? "" : name.trim();
        if (next.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        }
        Student student = requireCurrentStudent();
        if (student.getUser() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student user account is missing");
        }
        student.getUser().setName(next);
        userRepository.save(student.getUser());
        return student;
    }
}
