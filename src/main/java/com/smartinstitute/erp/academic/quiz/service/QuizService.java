package com.smartinstitute.erp.academic.quiz.service;

import com.smartinstitute.erp.academic.classroom.entity.Classroom;
import com.smartinstitute.erp.academic.classroom.repository.ClassroomRepository;
import com.smartinstitute.erp.academic.quiz.dto.*;
import com.smartinstitute.erp.academic.quiz.entity.*;
import com.smartinstitute.erp.academic.quiz.repository.*;
import com.smartinstitute.erp.academic.student.entity.Student;
import com.smartinstitute.erp.academic.student.repository.StudentRepository;
import com.smartinstitute.erp.academic.student.service.StudentAccountService;
import com.smartinstitute.erp.academic.subject.entity.Subject;
import com.smartinstitute.erp.academic.subject.repository.SubjectRepository;
import com.smartinstitute.erp.academic.teacher.entity.Teacher;
import com.smartinstitute.erp.academic.teacher.service.TeacherPermissionService;
import com.smartinstitute.erp.auth.entity.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final TeacherPermissionService teacherPermissionService;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final StudentQuizAttemptRepository attemptRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final ClassroomRepository classroomRepository;
    private final StudentAccountService studentAccountService;

    /** Quizzes for the current institute; teachers only see quizzes they may manage. */
    @Transactional(readOnly = true)
    public List<QuizResponseDto> listQuizzes() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Tenant not resolved from token (missing institute in session)");
        }
        List<Quiz> all = quizRepository.findByInstitute_Id(tenantId);
        if (studentAccountService.isStudentRole()) {
            Student s = studentAccountService.requireCurrentStudent();
            long cid = s.getClassroom().getId();
            return all.stream()
                    .filter(q -> q.getClassroom().getId().equals(cid))
                    .map(this::toQuizDto)
                    .toList();
        }
        if (!teacherPermissionService.isTeacherRole()) {
            return all.stream().map(this::toQuizDto).toList();
        }
        Teacher t = teacherPermissionService.getCurrentTeacher();
        return all.stream()
                .filter(q -> teacherPermissionService.canTeacherAccessQuiz(
                        t, tenantId, q.getSubject().getId(), q.getClassroom().getId()))
                .map(this::toQuizDto)
                .toList();
    }

    // CREATE QUIZ
    public QuizResponseDto createQuiz(QuizRequestDto dto){

        Subject subject = subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        Classroom classroom = classroomRepository.findById(dto.getClassroomId())
                .orElseThrow(() -> new RuntimeException("Classroom not found"));

        teacherPermissionService.assertCanManageQuiz(subject.getId(), classroom.getId());

        Quiz quiz = new Quiz();
        quiz.setTitle(dto.getTitle());
        quiz.setTotalMarks(dto.getTotalMarks());
        quiz.setDurationMinutes(dto.getDurationMinutes());
        quiz.setSubject(subject);
        quiz.setClassroom(classroom);

        Quiz saved = quizRepository.save(quiz);

        return toQuizDto(saved);
    }

    // GET QUIZ QUESTIONS
    public List<QuizQuestionResponseDto> getQuizQuestions(Long quizId){

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        if (studentAccountService.isStudentRole()) {
            Student s = studentAccountService.requireCurrentStudent();
            if (!quiz.getClassroom().getId().equals(s.getClassroom().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This exam is not for your class");
            }
        }

        return questionRepository.findByQuiz_Id(quizId)
                .stream()
                .map(this::toQuestionDto)
                .toList();
    }

    // UPDATE QUIZ
    public QuizResponseDto updateQuiz(Long id, QuizRequestDto dto){

        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        Subject subject = subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        Classroom classroom = classroomRepository.findById(dto.getClassroomId())
                .orElseThrow(() -> new RuntimeException("Classroom not found"));

        teacherPermissionService.assertCanManageQuiz(subject.getId(), classroom.getId());

        quiz.setTitle(dto.getTitle());
        quiz.setTotalMarks(dto.getTotalMarks());
        quiz.setDurationMinutes(dto.getDurationMinutes());
        quiz.setSubject(subject);
        quiz.setClassroom(classroom);

        Quiz updated = quizRepository.save(quiz);

        return toQuizDto(updated);
    }

    // DELETE QUIZ
    @Transactional
    public void deleteQuiz(Long id){

        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        teacherPermissionService.assertCanManageQuiz(
                quiz.getSubject().getId(), quiz.getClassroom().getId());

        quizRepository.deleteById(id);
    }

    // SUBMIT QUIZ
    public QuizResultResponseDto submitQuiz(Long quizId, Long studentId, List<AnswerDto> answers){

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        long resolvedId = studentId != null ? studentId : 0L;
        if (studentAccountService.isStudentRole()) {
            resolvedId = studentAccountService.requireCurrentStudent().getId();
        }

        Student student = studentRepository.findById(resolvedId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (studentAccountService.isStudentRole()
                && !quiz.getClassroom().getId().equals(student.getClassroom().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This exam is not for your class");
        }

        int score = 0;
        int totalMarks = 0;

        for(AnswerDto answer : answers){

            QuizQuestion question = questionRepository
                    .findById(answer.getQuestionId())
                    .orElseThrow(() -> new RuntimeException("Question not found"));

            // check question belongs to quiz
            if(!question.getQuiz().getId().equals(quizId)){
                throw new RuntimeException("Question does not belong to this quiz");
            }

            if(question.getCorrectAnswer()
                    .equalsIgnoreCase(answer.getSelectedAnswer())){

                score += question.getMarks();
            }

            totalMarks += question.getMarks();
        }

        StudentQuizAttempt attempt = new StudentQuizAttempt();
        attempt.setStudent(student);
        attempt.setQuiz(quiz);
        attempt.setScore(score);
        attempt.setTotalMarks(totalMarks);

        if(totalMarks > 0){
            attempt.setPercentage((score * 100.0) / totalMarks);
        } else {
            attempt.setPercentage(0.0);
        }

        StudentQuizAttempt saved = attemptRepository.save(attempt);

        return toResultDto(saved);
    }

    // GET STUDENT RESULTS
    public List<QuizResultResponseDto> getStudentResults(Long studentId){

        if (studentAccountService.isStudentRole()) {
            long mine = studentAccountService.requireCurrentStudent().getId();
            if (studentId == null || studentId != mine) {
                studentId = mine;
            }
        }

        return attemptRepository.findByStudent_Id(studentId)
                .stream()
                .map(this::toResultDto)
                .toList();
    }

    // CONVERT QUIZ ENTITY → DTO
    private QuizResponseDto toQuizDto(Quiz quiz){

        QuizResponseDto dto = new QuizResponseDto();

        dto.setId(quiz.getId());
        dto.setTitle(quiz.getTitle());
        dto.setTotalMarks(quiz.getTotalMarks());
        dto.setDurationMinutes(quiz.getDurationMinutes());
        dto.setSubjectName(quiz.getSubject().getName());
        dto.setClassroomName(quiz.getClassroom().getName());

        return dto;
    }

    // CONVERT QUESTION ENTITY → DTO (WITH SHUFFLED OPTIONS)
    private QuizQuestionResponseDto toQuestionDto(QuizQuestion question){

        QuizQuestionResponseDto dto = new QuizQuestionResponseDto();

        dto.setId(question.getId());
        dto.setQuestion(question.getQuestion());
        dto.setMarks(question.getMarks());

        List<String> options = new ArrayList<>();

        options.add(question.getOptionA());
        options.add(question.getOptionB());
        options.add(question.getOptionC());
        options.add(question.getOptionD());

        Collections.shuffle(options); // shuffle options

        dto.setOptions(options);

        return dto;
    }

    // CONVERT RESULT ENTITY → DTO
    private QuizResultResponseDto toResultDto(StudentQuizAttempt attempt){

        QuizResultResponseDto dto = new QuizResultResponseDto();

        dto.setStudentId(attempt.getStudent().getId());
        dto.setQuizId(attempt.getQuiz().getId());
        dto.setScore(attempt.getScore());
        dto.setTotalMarks(attempt.getTotalMarks());
        dto.setPercentage(attempt.getPercentage());

        return dto;
    }
}