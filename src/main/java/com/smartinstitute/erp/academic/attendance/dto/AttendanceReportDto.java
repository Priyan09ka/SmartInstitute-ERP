package com.smartinstitute.erp.academic.attendance.dto;

import lombok.Data;

@Data
public class AttendanceReportDto {

    private Long studentId;
    private String studentName;

    private long totalClasses;
    private long presentDays;
    private long absentDays;
    /** Days marked as holiday (excluded from attendance % denominator with absent). */
    private long holidayDays;

    private double percentage;

}
