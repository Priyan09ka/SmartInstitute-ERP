package com.smartinstitute.erp.academic.fee.entity;

import com.smartinstitute.erp.platform.institute.entity.Institute;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fee_payments")
@Data
@Filter(name = "tenantFilter", condition = "institute_id = :instituteId")
public class FeePayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fee_record_id", nullable = false)
    private FeeRecord feeRecord;

    @ManyToOne(optional = false)
    @JoinColumn(name = "institute_id", nullable = false)
    private Institute institute;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    private String note;

    @Column(nullable = false)
    private Instant paidAt = Instant.now();

    private String receiptNumber;
}
