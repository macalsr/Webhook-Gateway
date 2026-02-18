package com.mariaribeiro.webhookprocessor.webhook.port.out;

import com.mariaribeiro.webhookprocessor.webhook.domain.model.Source;

import java.util.Map;

public interface SignatureVerifier {
    boolean verify(Source source, String rawBody, String signature, Map<String, String> headers);
}