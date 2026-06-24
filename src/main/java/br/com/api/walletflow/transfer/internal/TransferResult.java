package br.com.api.walletflow.transfer.internal;

import java.math.BigDecimal;

record TransferResult(Long id, Long payerId, Long payeeId, BigDecimal amount) {
}
