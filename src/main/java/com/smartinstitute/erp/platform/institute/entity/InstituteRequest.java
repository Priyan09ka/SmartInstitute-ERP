package com.smartinstitute.erp.platform.institute.entity;

import com.smartinstitute.erp.platform.institute.RequestStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "institute_requests")
@Data
public class InstituteRequest {

    @Id
    @GeneratedValue
    private Long id;

    private String instituteName;
    private String adminName;
    private String adminEmail;
    private String address;

    private String password;
    private String phone;
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    private RequestStatus status; // PENDING, APPROVED, REJECTED

    private Instant createdAt = Instant.now();
}

