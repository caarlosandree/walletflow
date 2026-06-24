package br.com.api.walletflow.transfer.internal;

import br.com.api.walletflow.shared.Money;
import br.com.api.walletflow.transfer.TransferCompleted;
import br.com.api.walletflow.user.Account;
import br.com.api.walletflow.user.CommonUser;
import br.com.api.walletflow.user.Merchant;
import br.com.api.walletflow.user.UserDirectory;
import br.com.api.walletflow.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
class TransferService {

    private static final int MAX_ATTEMPTS = 5;

    private final UserDirectory userDirectory;
    private final WalletService walletService;
    private final AuthorizationClient authorizationClient;
    private final TransferRepository transferRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transferTransactionTemplate;

    TransferResult transfer(TransferCommand command) {
        Money amount = Money.of(command.value());
        if (amount.equals(Money.ZERO)) {
            throw new IllegalArgumentException("O valor da transferência deve ser positivo.");
        }

        // Identidade: pagador e recebedor devem existir (404 se não).
        Account payer = userDirectory.findById(command.payerId());
        userDirectory.findById(command.payeeId());

        // Regra de negócio via pattern matching exaustivo: lojista não envia.
        switch (payer) {
            case Merchant merchant -> throw new MerchantCannotSendException(merchant.id());
            case CommonUser ignored -> {
            }
        }

        // Idempotência (rápida): chave já processada?
        if (command.idempotencyKey() != null
                && transferRepository.existsByIdempotencyKey(command.idempotencyKey())) {
            throw new DuplicateTransferException(command.idempotencyKey());
        }

        // Autorizador externo — FORA da transação (não segura conexão do pool).
        if (!authorizationClient.isAuthorized()) {
            throw new TransferNotAuthorizedException();
        }

        return executeWithRetry(command, amount);
    }

    /** Retry no nível da orquestração: cada tentativa reabre a TX curta (dinheiro + evento). */
    private TransferResult executeWithRetry(TransferCommand command, Money amount) {
        int attempts = 0;
        while (true) {
            try {
                return transferTransactionTemplate.execute(status -> doTransfer(command, amount));
            } catch (OptimisticLockingFailureException ex) {
                if (++attempts >= MAX_ATTEMPTS) {
                    throw ex;
                }
            } catch (DataIntegrityViolationException ex) {
                // corrida de mesma chave de idempotência: a constraint UNIQUE reverteu a TX.
                throw new DuplicateTransferException(command.idempotencyKey());
            }
        }
    }

    private TransferResult doTransfer(TransferCommand command, Money amount) {
        walletService.transfer(command.payerId(), command.payeeId(), amount);

        TransferEntity entity = transferRepository.save(new TransferEntity(
                command.payerId(), command.payeeId(), amount.amount(), command.idempotencyKey()));

        // Mesma transação → o evento é gravado no outbox atomicamente com o movimento.
        eventPublisher.publishEvent(new TransferCompleted(
                entity.getId(), command.payerId(), command.payeeId(), amount.amount()));

        return new TransferResult(entity.getId(), command.payerId(), command.payeeId(), amount.amount());
    }
}
