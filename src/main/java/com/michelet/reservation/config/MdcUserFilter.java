package com.michelet.reservation.config;

import com.michelet.reservation.common.GatewayHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class MdcUserFilter extends OncePerRequestFilter {


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String userIdHeader = request.getHeader(GatewayHeaders.USER_ID);
        if (userIdHeader != null) {
            String userId = userIdHeader.trim();
            if (!userId.isBlank()) {
                MDC.put(MdcKeys.USER_ID, userId);
            }
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MdcKeys.USER_ID);
        }
    }
}
