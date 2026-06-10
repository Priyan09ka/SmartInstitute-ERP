package com.smartinstitute.erp.academic.attendance.dto;

import com.smartinstitute.erp.academic.attendance.entity.AttendanceStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AttendanceResponseDto {

    private Long id;

    private Long studentId;
    private String studentName;

    private Long classroomId;
    private String classroomName;

    private LocalDate date;

    private AttendanceStatus status;
}
