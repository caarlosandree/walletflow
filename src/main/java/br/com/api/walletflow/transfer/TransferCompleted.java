package br.com.api.walletflow.transfer;

import java.math.BigDecimal;

/**
 * Evento de domínio publicado quando uma transferência é concluída.
 *
 * <p>Gravado no outbox (Event Publication Registry) na mesma transação do
 * movimento de saldo. "Externalization-ready": basta anotar com
 * {@code @Externalized} para publicá-lo em um broker quando {@code notification}
 * sair do processo.
 */
public record TransferCompleted(Long transferId, Long payerId, Long payeeId, BigDecimal amount) {
}
