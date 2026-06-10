package com.smartinstitute.erp.academic.attendance.dto;

import com.smartinstitute.erp.academic.attendance.entity.AttendanceStatus;
import lombok.Data;

@Data
public class StudentAttendanceDto {

    private Long studentId;
    private AttendanceStatus status;
}
