package br.com.api.walletflow.transfer.internal;

import java.math.BigDecimal;

/** Comando interno de transferência (idempotencyKey opcional). */
record TransferCommand(Long payerId, Long payeeId, BigDecimal value, String idempotencyKey) {
}
