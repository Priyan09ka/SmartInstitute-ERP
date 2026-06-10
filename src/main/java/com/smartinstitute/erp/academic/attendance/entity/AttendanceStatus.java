package com.smartinstitute.erp.academic.attendance.entity;

public enum AttendanceStatus {
    PRESENT,
    ABSENT,
    /** Institute / class closed; does not count as absent for attendance %. */
    HOLIDAY
}
