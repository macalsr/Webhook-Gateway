package com.mariaribeiro.webhookprocessor.webhook.application;

import com.mariaribeiro.webhookprocessor.webhook.api.dto.WebhookAcceptedResponse;
import com.mariaribeiro.webhookprocessor.webhook.application.command.ReceiveWebhookCommand;

public interface ReceiveWebhookUseCase {

    WebhookAcceptedResponse handle(ReceiveWebhookCommand cmd);
}
