package com.mariaribeiro.webhookprocessor.webhook.api.security;

import com.mariaribeiro.webhookprocessor.config.WebhookProperties;
import com.mariaribeiro.webhookprocessor.webhook.infrastructure.crypto.HmacSha256Verifier;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Clock;

@Component
@RequiredArgsConstructor
public class WebhookSignatureInterceptor implements HandlerInterceptor {

    private final WebhookProperties webhookProperties;
    private final Clock clock;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String uri = request.getRequestURI();
        if(!uri.startsWith("/webhooks/")){
            return true;
        }
        String[] parts = uri.split("/");
        if (parts.length < 3){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }
        String source = parts[2];
        String signature = request.getHeader("X-Signature");
        String timeStamp = request.getHeader("X-Timestamp");

        if (signature == null || timeStamp == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        String rawBody = (String)  request.getAttribute("RAW_BODY");
        if(rawBody == null){
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return false;
        }

        String secret;

        try {
            secret = webhookProperties.secretFor(source);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return false;
        }

        String expectedHex;

        if(!timeStamp.isBlank()) {
            long ts = Long.parseLong(timeStamp);
            long now = clock.instant().getEpochSecond();

            if(Math.abs(now - ts) > 300){
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
            expectedHex = HmacSha256Verifier.signHex(secret, timeStamp + "." + rawBody);
        }else {
            expectedHex = HmacSha256Verifier.signHex(secret, rawBody);
        }

        String provided = signature.startsWith("sha256=") ? signature.substring("sha256=".length()) : signature;

        if(HmacSha256Verifier.constantTimeEquals(expectedHex, provided)){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        return true;
    }

}
