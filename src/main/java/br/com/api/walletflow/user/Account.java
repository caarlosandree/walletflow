package br.com.api.walletflow.user;

import br.com.api.walletflow.shared.Document;

/**
 * Visão de domínio de um usuário, exposta a outros módulos.
 *
 * <p>Tipo selado para permitir <em>pattern matching</em> exaustivo nas regras
 * de negócio (ex.: lojista não envia dinheiro). Não expõe dados sensíveis
 * (senha) nem a entidade JPA.
 */
public sealed interface Account permits CommonUser, Merchant {

    Long id();

    Document document();
}
