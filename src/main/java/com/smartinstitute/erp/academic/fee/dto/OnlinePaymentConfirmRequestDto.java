package com.smartinstitute.erp.academic.fee.dto;

import lombok.Data;

@Data
public class OnlinePaymentConfirmRequestDto {
    private String orderId;
    private String paymentId;
    private String signature;
    private String status; // SUCCESS | FAILED
    private String rawPayload;
}
