package com.mariaribeiro.webhookprocessor.webhook.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Component
public class RawBodyCaptureFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request, 1024 * 1024);
        filterChain.doFilter(wrapped, response);

        if(request.getRequestURI().startsWith("/webhooks/")) {
            byte[] buf = wrapped.getContentAsByteArray();
            String raw = new String(buf, StandardCharsets.UTF_8);
            request.setAttribute("RAW_BODY", raw);
        }
    }
}
