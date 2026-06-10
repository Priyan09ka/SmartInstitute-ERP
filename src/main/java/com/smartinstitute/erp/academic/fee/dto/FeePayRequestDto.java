package com.smartinstitute.erp.academic.fee.dto;

import com.smartinstitute.erp.academic.fee.entity.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FeePayRequestDto {
    private BigDecimal amount;
    private PaymentMethod method;
    private String note;
}
