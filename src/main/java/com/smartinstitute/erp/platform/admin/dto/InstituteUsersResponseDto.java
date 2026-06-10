package com.smartinstitute.erp.platform.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstituteUsersResponseDto {
    private Long instituteId;
    private String instituteName;
    private List<InstituteUserDto> students;
    private List<InstituteUserDto> teachers;
    private List<InstituteUserDto> principals;
}
