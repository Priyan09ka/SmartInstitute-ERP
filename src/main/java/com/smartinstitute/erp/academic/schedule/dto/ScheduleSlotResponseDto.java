package com.smartinstitute.erp.academic.schedule.dto;

import lombok.Data;

@Data
public class ScheduleSlotResponseDto {

    private Long id;
    private Long classroomId;
    private String classroomName;
    private Long teacherId;
    private String teacherName;
    private Long subjectId;
    private String subjectName;
    private String dayOfWeek;
    private String startTime;
    private String endTime;
}
