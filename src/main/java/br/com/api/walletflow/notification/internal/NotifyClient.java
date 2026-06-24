package br.com.api.walletflow.notification.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * Encapsula a chamada à API instável de notificação, com retry e backoff
 * exponencial (recurso nativo de resiliência do Spring Framework 7).
 */
@Component
@RequiredArgsConstructor
class NotifyClient {

    private final NotifyApi api;

    @Retryable(includes = RestClientException.class,
            maxRetries = 2,
            delay = 500,
            multiplier = 2,
            timeUnit = TimeUnit.MILLISECONDS)
    void notifyPayee(Long payeeId, BigDecimal amount) {
        api.send(new NotifyApi.NotificationPayload(payeeId, amount));
    }
}
