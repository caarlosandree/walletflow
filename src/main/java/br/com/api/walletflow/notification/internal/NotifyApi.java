package br.com.api.walletflow.notification.internal;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.math.BigDecimal;

/** Cliente declarativo da API de notificação (POST /notify) — instável. */
@HttpExchange(contentType = "application/json")
interface NotifyApi {

    @PostExchange("/notify")
    void send(@RequestBody NotificationPayload payload);

    record NotificationPayload(Long payeeId, BigDecimal amount) {
    }
}
