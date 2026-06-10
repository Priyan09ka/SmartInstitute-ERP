package com.smartinstitute.erp.academic.attendance.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BulkAttendanceRequestDto {
    private Long classroomId;
    private LocalDate date;
    private List<StudentAttendanceDto> students;
}
