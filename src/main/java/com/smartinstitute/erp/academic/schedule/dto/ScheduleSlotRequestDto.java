package com.smartinstitute.erp.academic.schedule.dto;

import lombok.Data;

@Data
public class ScheduleSlotRequestDto {

    private Long classroomId;
    private Long teacherId;
    private Long subjectId;
    /** MON, TUE, WED, THU, FRI, SAT, SUN */
    private String dayOfWeek;
    /** "HH:mm" or "HH:mm:ss" */
    private String startTime;
    private String endTime;
}
