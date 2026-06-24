package br.com.api.walletflow.user;

import br.com.api.walletflow.shared.Document;

/** Usuário comum: envia e recebe transferências. */
public record CommonUser(Long id, Document document) implements Account {
}
