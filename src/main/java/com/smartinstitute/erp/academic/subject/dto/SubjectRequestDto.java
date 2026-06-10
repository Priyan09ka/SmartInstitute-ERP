package com.smartinstitute.erp.academic.subject.dto;

import lombok.Data;

@Data
public class SubjectRequestDto {

    private String name;
    private String code;
    private Long courseId;

}