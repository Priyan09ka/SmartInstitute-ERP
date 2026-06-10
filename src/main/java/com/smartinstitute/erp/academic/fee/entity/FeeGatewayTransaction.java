package com.smartinstitute.erp.academic.fee.entity;

import com.smartinstitute.erp.platform.institute.entity.Institute;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fee_gateway_transactions")
@Data
@Filter(name = "tenantFilter", condition = "institute_id = :instituteId")
public class FeeGatewayTransaction {
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

    @Column(nullable = false, length = 8)
    private String currency = "INR";

    @Column(nullable = false, length = 40)
    private String gateway = "MOCKPAY";

    @Column(nullable = false, unique = true)
    private String gatewayOrderId;

    private String gatewayPaymentId;

    private String signature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GatewayTransactionStatus status = GatewayTransactionStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String rawPayload;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
