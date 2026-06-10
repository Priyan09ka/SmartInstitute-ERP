package com.smartinstitute.erp.user.dto;

import com.smartinstitute.erp.user.enums.Role;
import com.smartinstitute.erp.user.enums.Status;
import lombok.Data;

@Data
public class UserRequestDto {
    private String name;
    private String email;
    private String password;
    private Role role;
    private Status status;
}