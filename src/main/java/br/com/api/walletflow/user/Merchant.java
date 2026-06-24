package br.com.api.walletflow.user;

import br.com.api.walletflow.shared.Document;

/** Lojista: apenas recebe transferências. */
public record Merchant(Long id, Document document) implements Account {
}
