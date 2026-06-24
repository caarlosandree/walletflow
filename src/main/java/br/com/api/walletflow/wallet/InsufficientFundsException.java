package br.com.api.walletflow.wallet;

import br.com.api.walletflow.shared.Money;

/** Lançada quando o saldo do pagador não cobre o valor da transferência. */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(Long userId, Money balance, Money requested) {
        super("Saldo insuficiente na carteira do usuário %d: saldo %s, solicitado %s"
                .formatted(userId, balance.amount(), requested.amount()));
    }
}
