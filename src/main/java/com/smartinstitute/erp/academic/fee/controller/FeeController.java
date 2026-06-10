package com.smartinstitute.erp.academic.fee.controller;

import com.smartinstitute.erp.academic.fee.dto.FeeCreateRequestDto;
import com.smartinstitute.erp.academic.fee.dto.FeePayRequestDto;
import com.smartinstitute.erp.academic.fee.dto.FeePaymentResponseDto;
import com.smartinstitute.erp.academic.fee.dto.FeeResponseDto;
import com.smartinstitute.erp.academic.fee.dto.OnlinePaymentConfirmRequestDto;
import com.smartinstitute.erp.academic.fee.dto.OnlinePaymentInitRequestDto;
import com.smartinstitute.erp.academic.fee.dto.OnlinePaymentInitResponseDto;
import com.smartinstitute.erp.academic.fee.entity.FeeStatus;
import com.smartinstitute.erp.academic.fee.service.FeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fees")
@RequiredArgsConstructor
public class FeeController {
    private final FeeService feeService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public List<FeeResponseDto> myFees() {
        return feeService.listMyFees();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('INSTITUTE_ADMIN','PRINCIPAL')")
    public List<FeeResponseDto> list(@RequestParam(required = false) Long studentId,
                                     @RequestParam(required = false) FeeStatus status) {
        return feeService.listForInstitute(studentId, status);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTITUTE_ADMIN','PRINCIPAL')")
    public FeeResponseDto create(@RequestBody FeeCreateRequestDto dto) {
        return feeService.create(dto);
    }

    @PatchMapping("/{id}/mark-paid")
    @PreAuthorize("hasAnyRole('INSTITUTE_ADMIN','PRINCIPAL')")
    public FeeResponseDto markPaid(@PathVariable Long id) {
        return feeService.markPaid(id);
    }

    @PatchMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('INSTITUTE_ADMIN','PRINCIPAL')")
    public FeeResponseDto pay(@PathVariable Long id, @RequestBody FeePayRequestDto dto) {
        return feeService.pay(id, dto);
    }

    @GetMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('INSTITUTE_ADMIN','PRINCIPAL')")
    public List<FeePaymentResponseDto> payments(@PathVariable Long id) {
        return feeService.listPaymentsForInstitute(id);
    }

    @GetMapping("/my/{id}/payments")
    @PreAuthorize("hasRole('STUDENT')")
    public List<FeePaymentResponseDto> myPayments(@PathVariable Long id) {
        return feeService.listMyPayments(id);
    }

    @PostMapping("/my/{id}/online/init")
    @PreAuthorize("hasRole('STUDENT')")
    public OnlinePaymentInitResponseDto initOnlinePayment(@PathVariable Long id, @RequestBody(required = false) OnlinePaymentInitRequestDto dto) {
        return feeService.initMyOnlinePayment(id, dto);
    }

    @PostMapping("/my/{id}/online/confirm")
    @PreAuthorize("hasRole('STUDENT')")
    public FeeResponseDto confirmOnlinePayment(@PathVariable Long id, @RequestBody OnlinePaymentConfirmRequestDto dto) {
        return feeService.confirmMyOnlinePayment(id, dto);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @PreAuthorize("hasAnyRole('INSTITUTE_ADMIN','PRINCIPAL')")
    public ResponseEntity<String> exportInstituteCsv(@RequestParam(required = false) Long studentId,
                                                     @RequestParam(required = false) FeeStatus status) {
        String csv = feeService.exportInstituteCsv(studentId, status);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fees-export.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping(value = "/my/export", produces = "text/csv")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<String> exportMyCsv() {
        String csv = feeService.exportMyCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=my-fees.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping(value = "/{id}/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('INSTITUTE_ADMIN','PRINCIPAL')")
    public ResponseEntity<byte[]> receipt(@PathVariable Long id) {
        byte[] pdf = feeService.instituteReceiptPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fee-receipt-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping(value = "/my/{id}/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<byte[]> myReceipt(@PathVariable Long id) {
        byte[] pdf = feeService.myReceiptPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=my-fee-receipt-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
