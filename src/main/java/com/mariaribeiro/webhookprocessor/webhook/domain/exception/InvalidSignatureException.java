package com.mariaribeiro.webhookprocessor.webhook.domain.exception;

public class InvalidSignatureException extends RuntimeException {
    public InvalidSignatureException(String message) {
        super(message);
    }
}
