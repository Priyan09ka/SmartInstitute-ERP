package com.smartinstitute.erp.platform.institute.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "institutes")
@Data
public class Institute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String logoUrl;
    @Column(unique = true,nullable = false)
    private String address;
    private String phone;
    private String email;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
