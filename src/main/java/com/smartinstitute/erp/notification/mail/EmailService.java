package com.smartinstitute.erp.notification.mail;

public interface EmailService {
    void sendInstituteAdminCredentials(String toEmail, String password);
}
