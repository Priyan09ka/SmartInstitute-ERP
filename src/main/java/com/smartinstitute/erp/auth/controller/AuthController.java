package com.smartinstitute.erp.auth.controller;

import com.smartinstitute.erp.auth.dto.ChangePasswordRequestDto;
import com.smartinstitute.erp.auth.dto.ForgotPasswordRequestDto;
import com.smartinstitute.erp.auth.dto.LoginRequestDto;
import com.smartinstitute.erp.auth.dto.LoginResponseDto;
import com.smartinstitute.erp.auth.dto.ResetPasswordRequestDto;

import com.smartinstitute.erp.auth.dto.RefreshRequestDto;
import com.smartinstitute.erp.auth.service.AuthService;
import com.smartinstitute.erp.auth.service.JwtService;

import com.smartinstitute.erp.exception.TokenException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

   private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
            @RequestBody LoginRequestDto request,
            HttpServletResponse response){
        LoginResponseDto dto=authService.login(request);
        if (dto.isSuccess() && dto.getRefreshToken() != null && !dto.getRefreshToken().isBlank()) {
            Cookie cookie = JwtService.generateRefreshCookie(dto.getRefreshToken());
            response.addCookie(cookie);
        }
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refresh(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestBody RefreshRequestDto requestDto,
            @RequestHeader("User-Agent") String userAgent
    ) {
        String rawRefresh = null;

        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("refreshToken".equals(c.getName())) {
                    rawRefresh = c.getValue();
                    break;
                }
            }
        }

        if (rawRefresh == null) {
            throw new TokenException("Refresh token missing");
        }

        LoginResponseDto dto =
                authService.refresh(requestDto.getDeviceId(), userAgent, rawRefresh);

        // reset cookie with new refresh token
        Cookie newCookie = JwtService.generateRefreshCookie(dto.getRefreshToken());
        response.addCookie(newCookie);

        dto.setRefreshToken(null);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request,
                                    HttpServletResponse response){
        String rawRefresh=null;
        if (request.getCookies()!=null){
            for (Cookie cookie: request.getCookies()){
                if("refreshToken".equals(cookie.getName())){
                    rawRefresh= cookie.getValue();
                    break;
                }
            }
        }
        if (rawRefresh==null){
            return  ResponseEntity.ok("Already logged out");
        }
        authService.logout(rawRefresh);
        Cookie cookie=new Cookie("refreshToken",null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok("Logged out successfully!");
    }

    @PostMapping("/change-password")
    public void changePassword(@RequestBody ChangePasswordRequestDto request) {
        authService.changePassword(request);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody ForgotPasswordRequestDto request) {
        String message = authService.forgotPassword(request != null ? request.getEmail() : null);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ResetPasswordRequestDto request) {
        String message = authService.resetPassword(
                request != null ? request.getEmail() : null,
                request != null ? request.getCode() : null,
                request != null ? request.getNewPassword() : null
        );
        return ResponseEntity.ok(Map.of("message", message));
    }

}
