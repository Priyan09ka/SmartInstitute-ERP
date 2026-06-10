package com.smartinstitute.erp.academic.attendance.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AttendanceSummaryDto {
    private LocalDate date;
    private long presentCount;
    private long absentCount;
    private long holidayCount;
    private long totalMarked;
}
