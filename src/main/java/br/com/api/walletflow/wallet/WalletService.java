package br.com.api.walletflow.wallet;

import br.com.api.walletflow.shared.Money;

/**
 * Provided interface do módulo {@code wallet}: operações de saldo.
 *
 * <p>As carteiras são criadas sob demanda (lazy) na primeira operação para um
 * usuário, mantendo o módulo totalmente desacoplado de {@code user}.
 */
public interface WalletService {

    Money balanceOf(Long userId);

    /** Adiciona fundos à carteira (cria a carteira se necessário). */
    void deposit(Long userId, Money amount);

    /**
     * Move {@code amount} da carteira de {@code fromUserId} para {@code toUserId}
     * sob lock otimista, de forma atômica.
     *
     * @throws InsufficientFundsException se o saldo do pagador for insuficiente.
     */
    void transfer(Long fromUserId, Long toUserId, Money amount);
}
