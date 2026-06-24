package br.com.api.walletflow.transfer.internal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Payload de transferência (contrato do desafio: value, payer, payee). */
record TransferRequest(
        @NotNull @Positive BigDecimal value,
        @NotNull Long payer,
        @NotNull Long payee) {
}
