package com.smartinstitute.erp.notification.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LoggingMailService implements MailService {

        @Override
        public void sendStudentCredentials(String toEmail, String studentName, String plainPassword, String instituteName) {

            log.info("""
                Student credentials (mail not sent - configure spring.mail.* for real email)

                Institute : {}
                Student   : {}
                Email     : {}
                Password  : {}
                """,
                    instituteName, studentName, toEmail, plainPassword
            );
        }

        @Override
        public void sendPasswordResetCode(String toEmail, String displayName, String resetCode, int expiresInMinutes) {
            log.info("""
                Password reset code (mail not sent - configure spring.mail.* for real email)

                User       : {}
                Email      : {}
                Reset code : {}
                Expires in : {} minutes
                """,
                    displayName, toEmail, resetCode, expiresInMinutes
            );
        }
    }