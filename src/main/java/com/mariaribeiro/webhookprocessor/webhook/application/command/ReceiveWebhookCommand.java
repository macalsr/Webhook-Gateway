package com.mariaribeiro.webhookprocessor.webhook.application.command;

import java.time.Instant;
import java.util.Map;

public record ReceiveWebhookCommand(
        String source,
        String signature,
        String eventKey,
        String rawBody,
        Map<String,String> headers,
        Instant receivedAt
) {
}
