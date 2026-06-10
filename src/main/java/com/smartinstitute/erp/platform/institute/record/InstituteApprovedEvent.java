package com.smartinstitute.erp.platform.institute.record;

public record InstituteApprovedEvent(
        String email,
        String name,
        String instituteName
) {}

