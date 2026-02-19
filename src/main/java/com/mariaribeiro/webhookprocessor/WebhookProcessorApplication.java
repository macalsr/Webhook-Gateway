package com.mariaribeiro.webhookprocessor;

import com.mariaribeiro.webhookprocessor.config.WebhookProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WebhookProperties.class)
@SpringBootApplication
public class WebhookProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebhookProcessorApplication.class, args);
    }

}
