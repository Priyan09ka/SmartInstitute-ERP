package com.smartinstitute.erp.academic.attendance.controller;

import com.smartinstitute.erp.academic.attendance.dto.AttendanceReportDto;
import com.smartinstitute.erp.academic.attendance.dto.AttendanceRequestDto;
import com.smartinstitute.erp.academic.attendance.dto.AttendanceResponseDto;
import com.smartinstitute.erp.academic.attendance.dto.AttendanceSummaryDto;
import com.smartinstitute.erp.academic.attendance.dto.BulkAttendanceRequestDto;
import com.smartinstitute.erp.academic.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService service;

    @PostMapping
    public AttendanceResponseDto markAttendance(@RequestBody AttendanceRequestDto dto){
        return service.markAttendance(dto);
    }

    @GetMapping
    public List<AttendanceResponseDto> getAttendance(
            @RequestParam Long classroomId,
            @RequestParam LocalDate date){

        return service.getClassAttendance(classroomId,date);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public List<AttendanceResponseDto> myAttendance() {
        return service.listMyAttendance();
    }
    @PutMapping("/{id}")
    public AttendanceResponseDto update(
            @PathVariable Long id,
            @RequestBody AttendanceRequestDto dto){

        return service.update(id,dto);
    }
    @PostMapping("/bulk")
    public String markBulkAttendance(
            @RequestBody BulkAttendanceRequestDto dto) {

        service.markBulkAttendance(dto);

        return "Attendance marked successfully";
    }

    @GetMapping("/report/{studentId}")
    public AttendanceReportDto getStudentReport(
            @PathVariable Long studentId) {

        return service.getStudentAttendanceReport(studentId);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('INSTITUTE_ADMIN','PRINCIPAL')")
    public AttendanceSummaryDto summary(@RequestParam LocalDate date) {
        return service.getSummaryForDate(date);
    }
}