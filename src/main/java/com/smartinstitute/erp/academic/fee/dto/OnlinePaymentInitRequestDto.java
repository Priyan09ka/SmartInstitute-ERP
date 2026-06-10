package com.smartinstitute.erp.academic.fee.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OnlinePaymentInitRequestDto {
    private BigDecimal amount;
    private String gateway;
}
