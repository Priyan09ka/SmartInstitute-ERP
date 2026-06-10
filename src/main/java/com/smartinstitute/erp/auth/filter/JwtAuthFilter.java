package com.smartinstitute.erp.auth.filter;

import com.smartinstitute.erp.auth.entity.TenantContext;
import com.smartinstitute.erp.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final HibernateTenantFilter hibernateTenantFilter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        return path != null && (path.equals("/auth") || path.startsWith("/auth/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        boolean tenantFilterEnabled = false;
        safeClearTenant();

        try {

            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(7);

            if (!jwtService.validateAccessToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            String email = jwtService.extractEmail(token);
            String role = jwtService.extractRole(token);
            Long instituteId = jwtService.extractInstituteId(token);


            if (instituteId != null) {
                safeSetTenant(instituteId);
                hibernateTenantFilter.enableFilter(instituteId);
                tenantFilterEnabled = true;
            }

            SimpleGrantedAuthority authority =
                    new SimpleGrantedAuthority("ROLE_" + role);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            Collections.singleton(authority)
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } finally {
            if (tenantFilterEnabled) {
                hibernateTenantFilter.disable();
            }
            safeClearTenant();
        }
    }

    private void safeSetTenant(Long instituteId) {
        try {
            TenantContext.setTenantId(instituteId);
        } catch (Throwable ignored) {
            // Do not break auth response if tenant context initialization fails.
        }
    }

    private void safeClearTenant() {
        try {
            TenantContext.clear();
        } catch (Throwable ignored) {
            // Do not break response stream while clearing thread-local context.
        }
    }
}