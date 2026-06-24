package br.com.api.walletflow.transfer.internal;

/** Lançada quando uma chave de idempotência já foi processada. */
class DuplicateTransferException extends RuntimeException {

    DuplicateTransferException(String idempotencyKey) {
        super("Transferência já processada para a chave de idempotência: " + idempotencyKey);
    }
}
