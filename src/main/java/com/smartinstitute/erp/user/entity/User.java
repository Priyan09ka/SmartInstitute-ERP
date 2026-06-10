package com.smartinstitute.erp.user.entity;

import com.smartinstitute.erp.user.enums.Role;
import com.smartinstitute.erp.user.enums.Status;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false)
    private String name;

    @Column(unique = true,nullable = false)
    private  String email;

    @Column(nullable = false)
    private String password;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    private boolean firstLogin=true;
}
