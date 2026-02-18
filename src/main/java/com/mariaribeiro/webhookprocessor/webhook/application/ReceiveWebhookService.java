package com.mariaribeiro.webhookprocessor.webhook.application;

import com.mariaribeiro.webhookprocessor.webhook.api.dto.WebhookAcceptedResponse;
import com.mariaribeiro.webhookprocessor.webhook.application.command.ReceiveWebhookCommand;
import com.mariaribeiro.webhookprocessor.webhook.domain.exception.InvalidSignatureException;
import com.mariaribeiro.webhookprocessor.webhook.domain.model.Source;
import com.mariaribeiro.webhookprocessor.webhook.domain.model.WebhookEvent;
import com.mariaribeiro.webhookprocessor.webhook.port.out.SignatureVerifier;
import com.mariaribeiro.webhookprocessor.webhook.port.out.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class ReceiveWebhookService implements ReceiveWebhookUseCase {

    private final SignatureVerifier signatureVerifier;
    private final WebhookEventRepository repository;

    public ReceiveWebhookService(SignatureVerifier signatureVerifier, WebhookEventRepository repository) {
        this.signatureVerifier = signatureVerifier;
        this.repository = repository;
    }

    @Override
    public WebhookAcceptedResponse handle(ReceiveWebhookCommand cmd) {
        Source source = Source.from(cmd.source());

        boolean ok = signatureVerifier.verify(source,
                cmd.rawBody(),
                cmd.signature(),
                cmd.headers());

        if(!ok){
            throw new InvalidSignatureException(source.name());
        }

        if(cmd.eventKey() != null && !cmd.eventKey().isBlank()){
            Optional<WebhookEvent> existing = repository.findBySourceAndEventKey(source,cmd.eventKey());
            if (existing.isPresent()){
                return new WebhookAcceptedResponse(existing.get().id().toString(),"DUPLICATE");
            }
        }

        WebhookEvent event = WebhookEvent.newEvent(
                UUID.randomUUID(),
                source,
                cmd.eventKey(),
                cmd.rawBody(),
                cmd.receivedAt() != null ? cmd.receivedAt() : Instant.now()
        );

        repository.save(event);

        return new WebhookAcceptedResponse(event.id().toString(),"ACCEPTED");
    }
}
