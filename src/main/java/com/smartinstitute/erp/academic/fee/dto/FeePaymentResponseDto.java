package com.smartinstitute.erp.academic.fee.dto;

import com.smartinstitute.erp.academic.fee.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
public class FeePaymentResponseDto {
    private Long id;
    private Long feeRecordId;
    private BigDecimal amount;
    private PaymentMethod method;
    private String note;
    private Instant paidAt;
    private String receiptNumber;
}
