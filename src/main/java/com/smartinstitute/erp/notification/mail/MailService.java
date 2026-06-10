package com.smartinstitute.erp.notification.mail;

public interface MailService {
    void sendStudentCredentials(String toEmail, String studentName, String plainPassword,String instituteName);
    void sendPasswordResetCode(String toEmail, String displayName, String resetCode, int expiresInMinutes);
}
