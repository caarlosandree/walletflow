package br.com.api.walletflow.notification.internal;

import br.com.api.walletflow.transfer.TransferCompleted;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Reage a {@link TransferCompleted} de forma assíncrona e isolada.
 *
 * <p>{@code @ApplicationModuleListener} = {@code @Async} + {@code @TransactionalEventListener(AFTER_COMMIT)}
 * + {@code @Transactional(REQUIRES_NEW)}: a notificação só dispara após o commit
 * da transferência, roda em virtual thread e em transação própria — logo uma
 * falha aqui nunca reverte a transferência.
 */
@Component
@RequiredArgsConstructor
class NotificationListener {

    private final NotifyClient notifyClient;

    @ApplicationModuleListener
    void onTransferCompleted(TransferCompleted event) {
        notifyClient.notifyPayee(event.payeeId(), event.amount());
    }
}
