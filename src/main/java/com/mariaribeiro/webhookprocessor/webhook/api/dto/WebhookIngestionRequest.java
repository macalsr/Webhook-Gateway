package com.mariaribeiro.webhookprocessor.webhook.api.dto;

import tools.jackson.databind.JsonNode;

public record WebhookIngestionRequest(String eventKey, JsonNode payload) {
}
