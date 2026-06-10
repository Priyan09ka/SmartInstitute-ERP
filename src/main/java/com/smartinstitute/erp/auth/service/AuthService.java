package com.smartinstitute.erp.auth.service;

import com.smartinstitute.erp.auth.dto.ChangePasswordRequestDto;
import com.smartinstitute.erp.auth.dto.LoginRequestDto;
import com.smartinstitute.erp.auth.dto.LoginResponseDto;
import com.smartinstitute.erp.auth.entity.RefreshToken;
import com.smartinstitute.erp.auth.entity.PasswordResetToken;
import com.smartinstitute.erp.auth.repository.PasswordResetTokenRepository;
import com.smartinstitute.erp.auth.repository.RefreshTokenRepository;
import com.smartinstitute.erp.exception.UserNotLinkedException;
import com.smartinstitute.erp.notification.mail.MailService;
import com.smartinstitute.erp.user.entity.User;
import com.smartinstitute.erp.user.entity.UserInstitute;
import com.smartinstitute.erp.user.enums.Role;
import com.smartinstitute.erp.user.repository.UserInstituteRepository;
import com.smartinstitute.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.security.SecureRandom;
import java.util.UUID;

@Service
@RequiredArgsConstructor

public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserInstituteRepository userInstituteRepository;
    private final MailService mailService;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int RESET_CODE_EXP_MIN = 15;

    // ===== LOGIN =====
    public LoginResponseDto login(LoginRequestDto request) {
        if (request == null) {
            return LoginResponseDto.fail("Invalid login request");
        }
        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase(Locale.ROOT);
        String password = request.getPassword() == null ? "" : request.getPassword();
        String deviceId = request.getDeviceId() == null ? "unknown-device" : request.getDeviceId().trim();
        String userAgent = request.getUserAgent() == null ? "unknown-user-agent" : request.getUserAgent();

        if (email.isEmpty() || password.isEmpty()) {
            return LoginResponseDto.fail("Email and password are required");
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return LoginResponseDto.fail("Invalid email or password");
        }

        if (!encoder.matches(password, user.getPassword())) {
            return LoginResponseDto.fail("Invalid email or password");
        }

        String role;
        Long instituteId = null;

        if (user.getRole() == Role.SUPER_ADMIN) {
            role = Role.SUPER_ADMIN.name();
        } else {
            UserInstitute ui;
            try {
                ui = resolveInstituteLinkForLogin(user);
            } catch (UserNotLinkedException e) {
                return LoginResponseDto.fail("User is not linked to any institute. Please contact admin.");
            }
            role = ui.getRole().name();
            instituteId = ui.getInstitute().getId();
        }

        String accessToken = jwtService.generateAccessToken(
                user.getEmail(), role, instituteId
        );

        String jti = UUID.randomUUID().toString();
        String refreshToken = jwtService.generateRefreshToken(user.getEmail(), jti);

        RefreshToken rt = new RefreshToken();
        rt.setUserEmail(user.getEmail());
        rt.setJti(jti);
        rt.setTokenHash(DigestUtils.sha256Hex(refreshToken));
        rt.setDeviceId(deviceId.isEmpty() ? "unknown-device" : deviceId);
        rt.setUserAgentHash(DigestUtils.sha256Hex(Objects.toString(userAgent, "unknown-user-agent")));
        rt.setExpiresAt(Instant.now().plusMillis(14L * 24 * 60 * 60 * 1000));

        refreshTokenRepository.save(rt);


        return LoginResponseDto.success(user.getId(), user.getEmail(), role ,accessToken, refreshToken);

    }

    // ===== REFRESH =====
    public LoginResponseDto refresh(String deviceId, String userAgent, String rawRefreshToken) {

        RefreshToken old = jwtService
                .validateRefreshWithDevice(rawRefreshToken, deviceId, userAgent);

        old.setRevoked(true);

        String email = jwtService.extractEmailFromRefresh(rawRefreshToken);
        String newJti = UUID.randomUUID().toString();
        String newRefresh = jwtService.generateRefreshToken(email, newJti);

        RefreshToken nt = new RefreshToken();
        nt.setUserEmail(email);
        nt.setJti(newJti);
        nt.setTokenHash(jwtService.hashToken(newRefresh));
        nt.setDeviceId(deviceId);
        nt.setUserAgentHash(old.getUserAgentHash());
        nt.setExpiresAt(Instant.now().plusMillis(14L * 24 * 60 * 60 * 1000));

        old.setReplacedBy(newJti);

        refreshTokenRepository.save(nt);
        refreshTokenRepository.save(old);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User missing"));

        String role;
        Long instituteId = null;

        if (user.getRole() == Role.SUPER_ADMIN) {
            role = Role.SUPER_ADMIN.name();
        } else {
            UserInstitute ui = resolveInstituteLinkForLogin(user);
            role = ui.getRole().name();
            instituteId = ui.getInstitute().getId();
        }

        String newAccess = jwtService.generateAccessToken(
                user.getEmail(), role, instituteId
        );

        LoginResponseDto dto = new LoginResponseDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setRole(role);
        dto.setToken(newAccess);
        dto.setRefreshToken(newRefresh);
        dto.setSuccess(true);
        dto.setMessage("Token refreshed");

        return dto;
    }

    public void logout(String rawRefresh) {
        String hashed=jwtService.hashToken(rawRefresh);
        RefreshToken token=refreshTokenRepository.findByTokenHash(hashed).orElseThrow(()->new RuntimeException("Token not found"));
        if (token!=null){
            refreshTokenRepository.delete(token);
        }
    }
    public void changePassword(ChangePasswordRequestDto request) {

        User user = getCurrentUser();

        // 1. Validate password
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }

        // 2. Prevent same password reuse
        if (encoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("New password cannot be same as old password");
        }

        // 3. Update password
        user.setPassword(encoder.encode(request.getPassword()));
        user.setFirstLogin(false);

        userRepository.save(user);
    }

    public String forgotPassword(String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase(Locale.ROOT);
        if (email.isEmpty()) {
            return "If this email is registered, a reset code has been sent.";
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return "If this email is registered, a reset code has been sent.";
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        PasswordResetToken token = new PasswordResetToken();
        token.setEmail(user.getEmail().toLowerCase(Locale.ROOT));
        token.setTokenHash(DigestUtils.sha256Hex(code));
        token.setExpiresAt(Instant.now().plusSeconds(RESET_CODE_EXP_MIN * 60L));
        token.setUsed(false);
        passwordResetTokenRepository.save(token);

        mailService.sendPasswordResetCode(user.getEmail(), user.getName(), code, RESET_CODE_EXP_MIN);
        return "If this email is registered, a reset code has been sent.";
    }

    public String resetPassword(String rawEmail, String rawCode, String newPassword) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase(Locale.ROOT);
        String code = rawCode == null ? "" : rawCode.trim();
        String password = newPassword == null ? "" : newPassword.trim();

        if (email.isEmpty() || code.isEmpty() || password.isEmpty()) {
            throw new RuntimeException("Email, reset code, and new password are required");
        }
        if (password.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Invalid reset request"));

        PasswordResetToken token = passwordResetTokenRepository
                .findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset code"));

        if (token.isUsed() || token.getExpiresAt() == null || token.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Invalid or expired reset code");
        }
        if (!DigestUtils.sha256Hex(code).equals(token.getTokenHash())) {
            throw new RuntimeException("Invalid or expired reset code");
        }

        user.setPassword(encoder.encode(password));
        user.setFirstLogin(false);
        userRepository.save(user);
        refreshTokenRepository.deleteByUserEmail(user.getEmail());

        token.setUsed(true);
        passwordResetTokenRepository.save(token);
        return "Password reset successful. Please login.";
    }
    public User getCurrentUser() {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        String email = authentication.getName(); // JWT stores email as username

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Prefer the institute link whose role matches the user's account role so JWT {@code role} matches
     * {@code User.role} (avoids nondeterministic {@code findFirstByUser} when multiple links exist).
     */
    private UserInstitute resolveInstituteLinkForLogin(User user) {
        List<UserInstitute> links = userInstituteRepository.findAllByUserOrderByIdAsc(user);
        if (links.isEmpty()) {
            throw new UserNotLinkedException("User not linked to institute");
        }
        return links.stream()
                .filter(ui -> ui.getRole() == user.getRole())
                .findFirst()
                .orElseGet(() -> links.get(0));
    }
}
