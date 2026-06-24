package br.com.api.walletflow.notification.internal;

import br.com.api.walletflow.transfer.TransferCompleted;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ApplicationModuleTest
class NotificationModuleTest {

    @TestConfiguration
    static class StubConfig {
        @Bean
        @Primary
        RecordingNotifyApi recordingNotifyApi() {
            return new RecordingNotifyApi();
        }
    }

    static class RecordingNotifyApi implements NotifyApi {
        final AtomicInteger calls = new AtomicInteger();
        volatile NotificationPayload last;
        volatile boolean fail = false;

        @Override
        public void send(NotificationPayload payload) {
            calls.incrementAndGet();
            this.last = payload;
            if (fail) {
                throw new RestClientException("notificação indisponível");
            }
        }
    }

    @Autowired
    private RecordingNotifyApi notifyApi;

    @Test
    void notificaRecebedorAposEvento(Scenario scenario) {
        scenario.publish(new TransferCompleted(1L, 100L, 200L, new BigDecimal("30.00")))
                .andWaitForStateChange(() -> notifyApi.calls.get() > 0)
                .andVerify(__ -> {
                    assertEquals(200L, notifyApi.last.payeeId());
                    assertEquals(new BigDecimal("30.00"), notifyApi.last.amount());
                });
    }

    @Test
    void falhaNaNotificacaoNaoPropagaParaOPublicador(Scenario scenario) {
        notifyApi.fail = true;

        scenario.publish(new TransferCompleted(2L, 100L, 200L, new BigDecimal("10.00")))
                .andWaitForStateChange(() -> notifyApi.calls.get() > 0)
                .andVerify(__ -> assertTrue(notifyApi.calls.get() >= 1));
    }
}
