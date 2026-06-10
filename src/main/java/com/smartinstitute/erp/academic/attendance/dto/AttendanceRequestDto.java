package com.smartinstitute.erp.academic.attendance.dto;

import com.smartinstitute.erp.academic.attendance.entity.AttendanceStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AttendanceRequestDto {

    private Long studentId;
    private Long classroomId;
    private AttendanceStatus status;
    private LocalDate date;

}