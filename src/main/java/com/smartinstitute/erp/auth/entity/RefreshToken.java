package com.smartinstitute.erp.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "refresh_token")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true,nullable = false,length = 128)
    private String tokenHash;
    @Column(nullable = false)
    private String userEmail;
    @Column(nullable = false)
    private String jti;

    private Instant expiresAt;

    private boolean revoked=false;

    private String deviceId;

    private String userAgentHash;

    private String replacedBy;

    private Instant createdAt=Instant.now();
    private  Instant lastUsedAt;


}
