package com.smartinstitute.erp.user.dto;

import com.smartinstitute.erp.user.enums.Role;
import com.smartinstitute.erp.user.enums.Status;
import lombok.Data;

@Data
public class UserResponseDto {
    private long id;
    private String name;
    private String email;
    private Role role;
    private Status status;
}
