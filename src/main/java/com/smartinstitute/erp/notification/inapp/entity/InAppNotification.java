package com.smartinstitute.erp.notification.inapp.entity;

import com.smartinstitute.erp.platform.institute.entity.Institute;
import com.smartinstitute.erp.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Filter;

import java.time.Instant;

@Entity
@Table(name = "in_app_notifications")
@Data
@Filter(name = "tenantFilter", condition = "institute_id = :instituteId")
public class InAppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "institute_id")
    private Institute institute;

    @Column(nullable = false, length = 1024)
    private String message;

    @Column(nullable = false)
    private boolean readFlag;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
