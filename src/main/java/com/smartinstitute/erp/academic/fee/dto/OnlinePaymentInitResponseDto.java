package com.smartinstitute.erp.academic.fee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class OnlinePaymentInitResponseDto {
    private String gateway;
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String paymentUrl;
}
