package com.smartinstitute.erp.auth.filter;

import jakarta.servlet.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TenantCleanupFilter implements Filter {

    private final HibernateTenantFilter hibernateTenantFilter;

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } finally {
            hibernateTenantFilter.disable();  // ✅ important
        }
    }
}