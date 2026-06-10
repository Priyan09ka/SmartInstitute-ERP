package com.smartinstitute.erp.academic.fee.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FeeCreateRequestDto {
    private Long studentId;
    private String title;
    private BigDecimal amount;
    private LocalDate dueDate;
}
