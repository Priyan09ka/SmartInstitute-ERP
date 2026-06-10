package com.smartinstitute.erp.academic.fee.dto;

import com.smartinstitute.erp.academic.fee.entity.FeeStatus;
import com.smartinstitute.erp.academic.fee.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class FeeResponseDto {
    private Long id;
    private Long studentId;
    private String studentName;
    private String rollNumber;
    private String title;
    private BigDecimal amount;
    private BigDecimal amountPaid;
    private BigDecimal balanceAmount;
    private LocalDate dueDate;
    private FeeStatus status;
    private PaymentMethod lastPaymentMethod;
    private Instant paidAt;
    private String receiptNumber;

    // Backward-compatible constructor for older call sites.
    public FeeResponseDto(
            Long id,
            Long studentId,
            String studentName,
            String rollNumber,
            String title,
            BigDecimal amount,
            LocalDate dueDate,
            FeeStatus status,
            Instant paidAt,
            String receiptNumber
    ) {
        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.rollNumber = rollNumber;
        this.title = title;
        this.amount = amount;
        this.amountPaid = BigDecimal.ZERO;
        this.balanceAmount = amount;
        this.dueDate = dueDate;
        this.status = status;
        this.lastPaymentMethod = null;
        this.paidAt = paidAt;
        this.receiptNumber = receiptNumber;
    }
}
