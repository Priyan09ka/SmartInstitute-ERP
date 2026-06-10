package com.smartinstitute.erp.academic.fee.service;

import com.smartinstitute.erp.academic.fee.dto.FeeCreateRequestDto;
import com.smartinstitute.erp.academic.fee.dto.OnlinePaymentConfirmRequestDto;
import com.smartinstitute.erp.academic.fee.dto.OnlinePaymentInitRequestDto;
import com.smartinstitute.erp.academic.fee.dto.OnlinePaymentInitResponseDto;
import com.smartinstitute.erp.academic.fee.dto.FeePayRequestDto;
import com.smartinstitute.erp.academic.fee.dto.FeePaymentResponseDto;
import com.smartinstitute.erp.academic.fee.dto.FeeResponseDto;
import com.smartinstitute.erp.academic.fee.entity.FeeGatewayTransaction;
import com.smartinstitute.erp.academic.fee.entity.FeePayment;
import com.smartinstitute.erp.academic.fee.entity.FeeRecord;
import com.smartinstitute.erp.academic.fee.entity.FeeStatus;
import com.smartinstitute.erp.academic.fee.entity.GatewayTransactionStatus;
import com.smartinstitute.erp.academic.fee.entity.PaymentMethod;
import com.smartinstitute.erp.academic.fee.repository.FeeGatewayTransactionRepository;
import com.smartinstitute.erp.academic.fee.repository.FeePaymentRepository;
import com.smartinstitute.erp.academic.fee.repository.FeeRecordRepository;
import com.smartinstitute.erp.academic.student.entity.Student;
import com.smartinstitute.erp.academic.student.repository.StudentRepository;
import com.smartinstitute.erp.academic.student.service.StudentAccountService;
import com.smartinstitute.erp.auth.entity.TenantContext;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeeService {
    private final FeeRecordRepository feeRecordRepository;
    private final FeePaymentRepository feePaymentRepository;
    private final FeeGatewayTransactionRepository feeGatewayTransactionRepository;
    private final StudentRepository studentRepository;
    private final StudentAccountService studentAccountService;

    @Transactional
    public FeeResponseDto create(FeeCreateRequestDto dto) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant not resolved");
        }
        if (dto == null || dto.getStudentId() == null || dto.getTitle() == null || dto.getTitle().isBlank()
                || dto.getAmount() == null || dto.getDueDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studentId, title, amount, and dueDate are required");
        }

        Student student = studentRepository.findByIdAndInstitute_Id(dto.getStudentId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student not found in your institute"));

        FeeRecord fee = new FeeRecord();
        fee.setStudent(student);
        fee.setInstitute(student.getInstitute());
        fee.setTitle(dto.getTitle().trim());
        fee.setAmount(dto.getAmount());
        fee.setAmountPaid(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        fee.setDueDate(dto.getDueDate());
        fee.setStatus(FeeStatus.UNPAID);
        feeRecordRepository.save(fee);
        return toDto(fee);
    }

    public List<FeeResponseDto> listForInstitute(Long studentId) {
        return listForInstitute(studentId, null);
    }

    public List<FeeResponseDto> listForInstitute(Long studentId, FeeStatus status) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant not resolved");
        }
        List<FeeRecord> list;
        if (studentId != null) {
            list = feeRecordRepository.findAllByInstitute_IdAndStudent_IdOrderByDueDateAsc(tenantId, studentId);
            if (status != null) {
                list = list.stream().filter(x -> x.getStatus() == status).toList();
            }
        } else if (status != null) {
            list = feeRecordRepository.findAllByInstitute_IdAndStatusOrderByDueDateAsc(tenantId, status);
        } else {
            list = feeRecordRepository.findAllByInstitute_IdOrderByDueDateAsc(tenantId);
        }
        return list.stream().map(this::toDto).toList();
    }

    public List<FeeResponseDto> listMyFees() {
        Student me = studentAccountService.requireCurrentStudent();
        return feeRecordRepository
                .findAllByInstitute_IdAndStudent_IdOrderByDueDateAsc(me.getInstitute().getId(), me.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public FeeResponseDto markPaid(Long feeId) {
        FeePayRequestDto dto = new FeePayRequestDto();
        dto.setAmount(null);
        dto.setMethod(PaymentMethod.OTHER);
        dto.setNote("Marked as paid by admin");
        return pay(feeId, dto);
    }

    @Transactional
    public FeeResponseDto pay(Long feeId, FeePayRequestDto dto) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant not resolved");
        }
        FeeRecord fee = feeRecordRepository.findById(feeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fee record not found"));
        if (fee.getInstitute() == null || fee.getInstitute().getId() == null || !tenantId.equals(fee.getInstitute().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Fee record not in your institute");
        }
        BigDecimal total = safeMoney(fee.getAmount());
        BigDecimal alreadyPaid = safeMoney(fee.getAmountPaid());
        BigDecimal remaining = total.subtract(alreadyPaid).max(BigDecimal.ZERO);
        BigDecimal payAmount = dto != null && dto.getAmount() != null ? safeMoney(dto.getAmount()) : remaining;
        PaymentMethod method = dto != null && dto.getMethod() != null ? dto.getMethod() : PaymentMethod.OTHER;
        return applyPayment(fee, payAmount, method, dto != null ? dto.getNote() : null);
    }

    @Transactional
    public OnlinePaymentInitResponseDto initMyOnlinePayment(Long feeId, OnlinePaymentInitRequestDto dto) {
        Student me = studentAccountService.requireCurrentStudent();
        FeeRecord fee = feeRecordRepository
                .findByIdAndInstitute_IdAndStudent_Id(feeId, me.getInstitute().getId(), me.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fee record not found"));
        BigDecimal total = safeMoney(fee.getAmount());
        BigDecimal alreadyPaid = safeMoney(fee.getAmountPaid());
        BigDecimal remaining = total.subtract(alreadyPaid).max(BigDecimal.ZERO);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fee is already fully paid");
        }
        BigDecimal requested = dto != null && dto.getAmount() != null ? safeMoney(dto.getAmount()) : remaining;
        if (requested.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than 0");
        }
        if (requested.compareTo(remaining) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount exceeds remaining balance");
        }
        String gateway = dto != null && dto.getGateway() != null && !dto.getGateway().isBlank()
                ? dto.getGateway().trim().toUpperCase()
                : "MOCKPAY";
        String orderId = "ORD-" + System.currentTimeMillis() + "-" + fee.getId();

        FeeGatewayTransaction tx = new FeeGatewayTransaction();
        tx.setFeeRecord(fee);
        tx.setInstitute(fee.getInstitute());
        tx.setAmount(requested);
        tx.setCurrency("INR");
        tx.setGateway(gateway);
        tx.setGatewayOrderId(orderId);
        tx.setStatus(GatewayTransactionStatus.PENDING);
        tx.setCreatedAt(Instant.now());
        tx.setUpdatedAt(Instant.now());
        feeGatewayTransactionRepository.save(tx);

        return new OnlinePaymentInitResponseDto(
                gateway,
                orderId,
                requested,
                "INR",
                "PENDING",
                "/payments/mock/checkout?orderId=" + orderId
        );
    }

    @Transactional
    public FeeResponseDto confirmMyOnlinePayment(Long feeId, OnlinePaymentConfirmRequestDto dto) {
        Student me = studentAccountService.requireCurrentStudent();
        if (dto == null || dto.getOrderId() == null || dto.getOrderId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId is required");
        }
        FeeRecord fee = feeRecordRepository
                .findByIdAndInstitute_IdAndStudent_Id(feeId, me.getInstitute().getId(), me.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fee record not found"));
        FeeGatewayTransaction tx = feeGatewayTransactionRepository
                .findByGatewayOrderIdAndInstitute_Id(dto.getOrderId().trim(), me.getInstitute().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gateway order not found"));
        if (tx.getFeeRecord() == null || !fee.getId().equals(tx.getFeeRecord().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gateway order does not match this fee");
        }
        if (tx.getStatus() == GatewayTransactionStatus.SUCCESS) {
            return toDto(fee);
        }

        String status = dto.getStatus() == null ? "SUCCESS" : dto.getStatus().trim().toUpperCase();
        tx.setGatewayPaymentId(dto.getPaymentId());
        tx.setSignature(dto.getSignature());
        tx.setRawPayload(dto.getRawPayload());
        tx.setUpdatedAt(Instant.now());

        if ("FAILED".equals(status)) {
            tx.setStatus(GatewayTransactionStatus.FAILED);
            feeGatewayTransactionRepository.save(tx);
            return toDto(fee);
        }

        tx.setStatus(GatewayTransactionStatus.SUCCESS);
        feeGatewayTransactionRepository.save(tx);

        return applyPayment(
                fee,
                safeMoney(tx.getAmount()),
                PaymentMethod.ONLINE,
                "Online payment via " + safe(tx.getGateway()) + " (" + safe(tx.getGatewayOrderId()) + ")"
        );
    }

    public List<FeePaymentResponseDto> listPaymentsForInstitute(Long feeId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant not resolved");
        return feePaymentRepository.findAllByFeeRecord_IdAndInstitute_IdOrderByPaidAtDesc(feeId, tenantId)
                .stream().map(this::toPaymentDto).toList();
    }

    public List<FeePaymentResponseDto> listMyPayments(Long feeId) {
        Student me = studentAccountService.requireCurrentStudent();
        FeeRecord fee = feeRecordRepository.findByIdAndInstitute_IdAndStudent_Id(feeId, me.getInstitute().getId(), me.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fee record not found"));
        return feePaymentRepository.findAllByFeeRecord_IdAndInstitute_IdOrderByPaidAtDesc(fee.getId(), me.getInstitute().getId())
                .stream().map(this::toPaymentDto).toList();
    }

    public String exportInstituteCsv(Long studentId, FeeStatus status) {
        List<FeeResponseDto> rows = listForInstitute(studentId, status);
        return toCsv(rows);
    }

    public String exportMyCsv() {
        List<FeeResponseDto> rows = listMyFees();
        return toCsv(rows);
    }

    public String instituteReceiptText(Long feeId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant not resolved");
        }
        FeeRecord fee = feeRecordRepository.findById(feeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fee record not found"));
        if (fee.getInstitute() == null || !tenantId.equals(fee.getInstitute().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Fee record not in your institute");
        }
        return toReceiptText(toDto(fee));
    }

    public String myReceiptText(Long feeId) {
        Student me = studentAccountService.requireCurrentStudent();
        FeeRecord fee = feeRecordRepository
                .findByIdAndInstitute_IdAndStudent_Id(feeId, me.getInstitute().getId(), me.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fee record not found"));
        return toReceiptText(toDto(fee));
    }

    public byte[] instituteReceiptPdf(Long feeId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant not resolved");
        }
        FeeRecord fee = feeRecordRepository.findById(feeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fee record not found"));
        if (fee.getInstitute() == null || !tenantId.equals(fee.getInstitute().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Fee record not in your institute");
        }
        return toReceiptPdf(fee, toDto(fee));
    }

    public byte[] myReceiptPdf(Long feeId) {
        Student me = studentAccountService.requireCurrentStudent();
        FeeRecord fee = feeRecordRepository
                .findByIdAndInstitute_IdAndStudent_Id(feeId, me.getInstitute().getId(), me.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fee record not found"));
        return toReceiptPdf(fee, toDto(fee));
    }

    private String generateReceiptNumber(FeeRecord fee) {
        String date = DateTimeFormatter.BASIC_ISO_DATE.format(java.time.LocalDate.now());
        String idPart = fee.getId() == null ? String.valueOf(System.currentTimeMillis()) : String.format("%06d", fee.getId());
        return "RCPT-" + date + "-" + idPart;
    }

    private FeeResponseDto applyPayment(FeeRecord fee, BigDecimal payAmount, PaymentMethod method, String note) {
        BigDecimal total = safeMoney(fee.getAmount());
        BigDecimal alreadyPaid = safeMoney(fee.getAmountPaid());
        BigDecimal remaining = total.subtract(alreadyPaid).max(BigDecimal.ZERO);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return toDto(fee);
        }
        if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount must be greater than 0");
        }
        if (payAmount.compareTo(remaining) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount cannot exceed remaining balance");
        }

        String receiptNo = generateReceiptNumber(fee);

        FeePayment payment = new FeePayment();
        payment.setFeeRecord(fee);
        payment.setInstitute(fee.getInstitute());
        payment.setAmount(payAmount);
        payment.setMethod(method != null ? method : PaymentMethod.OTHER);
        payment.setNote(note);
        payment.setPaidAt(Instant.now());
        payment.setReceiptNumber(receiptNo);
        feePaymentRepository.save(payment);

        BigDecimal nextPaid = alreadyPaid.add(payAmount).setScale(2, RoundingMode.HALF_UP);
        fee.setAmountPaid(nextPaid);
        fee.setLastPaymentMethod(method != null ? method : PaymentMethod.OTHER);
        fee.setPaidAt(payment.getPaidAt());
        if (nextPaid.compareTo(total) >= 0) {
            fee.setStatus(FeeStatus.PAID);
            if (fee.getReceiptNumber() == null || fee.getReceiptNumber().isBlank()) {
                fee.setReceiptNumber(receiptNo);
            }
        } else {
            fee.setStatus(FeeStatus.PARTIALLY_PAID);
        }
        feeRecordRepository.save(fee);
        return toDto(fee);
    }

    private String toCsv(List<FeeResponseDto> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,studentName,rollNumber,title,totalAmount,amountPaid,balanceAmount,dueDate,status,lastPaymentMethod,paidAt,receiptNumber\n");
        for (FeeResponseDto r : rows) {
            sb.append(csv(r.getId()))
                    .append(",").append(csv(r.getStudentName()))
                    .append(",").append(csv(r.getRollNumber()))
                    .append(",").append(csv(r.getTitle()))
                    .append(",").append(csv(r.getAmount()))
                    .append(",").append(csv(r.getAmountPaid()))
                    .append(",").append(csv(r.getBalanceAmount()))
                    .append(",").append(csv(r.getDueDate()))
                    .append(",").append(csv(r.getStatus()))
                    .append(",").append(csv(r.getLastPaymentMethod()))
                    .append(",").append(csv(r.getPaidAt()))
                    .append(",").append(csv(r.getReceiptNumber()))
                    .append("\n");
        }
        return sb.toString();
    }

    private String toReceiptText(FeeResponseDto r) {
        return """
                Smart Institute ERP - Fee Receipt
                ---------------------------------
                Receipt No : %s
                Student    : %s
                Roll No    : %s
                Title      : %s
                Total      : Rs. %s
                Paid       : Rs. %s
                Balance    : Rs. %s
                Due Date   : %s
                Status     : %s
                Method     : %s
                Paid At    : %s
                """
                .formatted(
                        safe(r.getReceiptNumber()),
                        safe(r.getStudentName()),
                        safe(r.getRollNumber()),
                        safe(r.getTitle()),
                        r.getAmount() != null ? r.getAmount().toPlainString() : "0.00",
                        r.getAmountPaid() != null ? r.getAmountPaid().toPlainString() : "0.00",
                        r.getBalanceAmount() != null ? r.getBalanceAmount().toPlainString() : "0.00",
                        safe(r.getDueDate()),
                        safe(r.getStatus()),
                        safe(r.getLastPaymentMethod()),
                        safe(r.getPaidAt())
                );
    }

    private byte[] toReceiptPdf(FeeRecord fee, FeeResponseDto dto) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float left = 50;
                float top = page.getMediaBox().getHeight() - 60;
                float y = top;
                float line = 18;
                PDFont fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                PDFont fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

                y = writeLine(content, left, y, fontBold, 16, "Smart Institute ERP");
                y = writeLine(content, left, y - 2, fontBold, 14, "Fee Receipt");
                y = writeLine(content, left, y - 8, fontRegular, 11, "Generated: " + Instant.now());
                y = writeLine(content, left, y - 10, fontBold, 12, "----------------------------------------");

                String instituteName = fee.getInstitute() != null ? fee.getInstitute().getName() : null;
                y -= line;
                y = writeKeyValue(content, left, y, "Institute", safe(instituteName), fontBold, fontRegular);
                y = writeKeyValue(content, left, y, "Receipt No", safe(dto.getReceiptNumber()), fontBold, fontRegular);
                y = writeKeyValue(content, left, y, "Student", safe(dto.getStudentName()), fontBold, fontRegular);
                y = writeKeyValue(content, left, y, "Roll No", safe(dto.getRollNumber()), fontBold, fontRegular);
                y = writeKeyValue(content, left, y, "Title", safe(dto.getTitle()), fontBold, fontRegular);
                y = writeKeyValue(content, left, y, "Total", "Rs. " + money(dto.getAmount()), fontBold, fontRegular);
                y = writeKeyValue(content, left, y, "Paid", "Rs. " + money(dto.getAmountPaid()), fontBold, fontRegular);
                y = writeKeyValue(content, left, y, "Balance", "Rs. " + money(dto.getBalanceAmount()), fontBold, fontRegular);
                y = writeKeyValue(content, left, y, "Due Date", safe(dto.getDueDate()), fontBold, fontRegular);
                y = writeKeyValue(content, left, y, "Status", safe(dto.getStatus()), fontBold, fontRegular);
                y = writeKeyValue(content, left, y, "Method", safe(dto.getLastPaymentMethod()), fontBold, fontRegular);
                y = writeKeyValue(content, left, y, "Paid At", safe(dto.getPaidAt()), fontBold, fontRegular);
            }

            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate receipt PDF");
        }
    }

    private float writeKeyValue(PDPageContentStream content, float x, float y, String key, String value, PDFont fontBold, PDFont fontRegular) throws IOException {
        writeLine(content, x, y, fontBold, 11, key + " :");
        return writeLine(content, x + 120, y, fontRegular, 11, value) - 6;
    }

    private float writeLine(PDPageContentStream content, float x, float y, PDFont font, int size, String text) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(text == null ? "" : text);
        content.endText();
        return y - (size + 2);
    }

    private String money(BigDecimal value) {
        return safeMoney(value).toPlainString();
    }

    private BigDecimal safeMoney(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String csv(Object o) {
        String s = o == null ? "" : String.valueOf(o);
        String escaped = s.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private String safe(Object o) {
        return o == null ? "-" : String.valueOf(o);
    }

    private FeePaymentResponseDto toPaymentDto(FeePayment p) {
        return new FeePaymentResponseDto(
                p.getId(),
                p.getFeeRecord() != null ? p.getFeeRecord().getId() : null,
                p.getAmount(),
                p.getMethod(),
                p.getNote(),
                p.getPaidAt(),
                p.getReceiptNumber()
        );
    }

    private FeeResponseDto toDto(FeeRecord fee) {
        BigDecimal total = safeMoney(fee.getAmount());
        BigDecimal paid = safeMoney(fee.getAmountPaid());
        BigDecimal balance = total.subtract(paid).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return new FeeResponseDto(
                fee.getId(),
                fee.getStudent() != null ? fee.getStudent().getId() : null,
                fee.getStudent() != null && fee.getStudent().getUser() != null ? fee.getStudent().getUser().getName() : null,
                fee.getStudent() != null ? fee.getStudent().getRollNumber() : null,
                fee.getTitle(),
                total,
                paid,
                balance,
                fee.getDueDate(),
                fee.getStatus(),
                fee.getLastPaymentMethod(),
                fee.getPaidAt(),
                fee.getReceiptNumber()
        );
    }
}
