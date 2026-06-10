package com.smartinstitute.erp.user.entity;

import com.smartinstitute.erp.platform.institute.entity.Institute;
import com.smartinstitute.erp.user.enums.Role;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "user_institutes")
@Data
public class UserInstitute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "institute_id")
    private Institute institute;

    @Enumerated(EnumType.STRING)
    private Role role;
}
