package com.schoolms.filter;

import com.schoolms.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TenantFilter extends HttpFilter {
    public static final String HEADER = "X-School-Id";

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        String tenant = req.getHeader(HEADER);
        try {
            if (tenant != null) {
                try {
                    Long id = Long.parseLong(tenant);
                    TenantContext.setCurrentTenant(id);
                } catch (NumberFormatException ignored) {
                }
            }
            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();
        }
    }
}
