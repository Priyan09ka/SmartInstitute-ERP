package com.smartinstitute.erp.notification.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@Primary
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
public class SmtpMailService implements MailService {

    private final JavaMailSender mailSender;

    @Value("${erp.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${erp.mail.from:${spring.mail.username:noreply@smartinstitute.local}}")
    private String fromAddress;

    /** Frontend login URL (shown in the welcome email). */
    @Value("${erp.app.login-url:http://localhost:5173}")
    private String loginUrl;

    @Override
    public void sendStudentCredentials(String toEmail, String studentName, String plainPassword, String instituteName) {

        if (!mailEnabled) {
            log.info("Mail disabled: would send credentials to {} (name={})", toEmail, studentName);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Your Student Login Credentials - " + instituteName);

            helper.setText(buildCredentialsBody(studentName, toEmail, plainPassword, instituteName, loginUrl), true);

            mailSender.send(message);

            log.info("Credentials email sent to {}", toEmail);

        } catch (MailException e) {
            log.warn("Could not send credentials email to {} (check spring.mail.* / app password): {}", toEmail, e.getMessage());
        } catch (Throwable t) {
            // Covers MessagingException, Angus mail runtime errors, etc.
            log.warn("Could not send credentials email to {}: {}", toEmail, t.getMessage());
        }
    }

    @Override
    public void sendPasswordResetCode(String toEmail, String displayName, String resetCode, int expiresInMinutes) {
        if (!mailEnabled) {
            log.info("Mail disabled: would send password reset code to {} (name={})", toEmail, displayName);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Password reset code");
            helper.setText(buildPasswordResetBody(displayName, resetCode, expiresInMinutes), true);
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (MailException e) {
            log.warn("Could not send password reset email to {}: {}", toEmail, e.getMessage());
        } catch (Throwable t) {
            log.warn("Could not send password reset email to {}: {}", toEmail, t.getMessage());
        }
    }

    private String buildCredentialsBody(String studentName, String email, String password, String instituteName, String loginPageUrl) {
        String url = loginPageUrl != null ? loginPageUrl.trim() : "";
        String loginBlock = url.isEmpty()
                ? "<p>Use your institute’s student login page to sign in.</p>"
                : "<p><a href=\"" + url.replace("\"", "&quot;") + "\">Open login page</a></p>";
        return """
        <p>Hello %s,</p>

        <p>Welcome to <strong>%s</strong>.</p>

        <p>Your student account has been created. Sign in with:</p>

        <ul>
            <li><strong>Email:</strong> %s</li>
            <li><strong>Temporary password:</strong> %s</li>
        </ul>

        <p><strong>Important:</strong> You must change your password after your first login.</p>

        %s

        <p>Regards,<br>
        %s</p>
        """.formatted(studentName, instituteName, email, password, loginBlock, instituteName);
    }

    private String buildPasswordResetBody(String name, String resetCode, int expiresInMinutes) {
        String safeName = (name == null || name.isBlank()) ? "User" : name;
        return """
        <p>Hello %s,</p>
        <p>We received a request to reset your account password.</p>
        <p>Your reset code is:</p>
        <p style="font-size:24px;font-weight:700;letter-spacing:2px;">%s</p>
        <p>This code expires in %d minutes.</p>
        <p>If you did not request a password reset, you can ignore this email.</p>
        """.formatted(safeName, resetCode, expiresInMinutes);
    }
}
