package br.com.api.walletflow.transfer.internal;

import java.math.BigDecimal;

record TransferResponse(Long id, Long payer, Long payee, BigDecimal value) {
}
