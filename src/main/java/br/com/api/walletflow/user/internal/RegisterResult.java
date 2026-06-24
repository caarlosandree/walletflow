package br.com.api.walletflow.user.internal;

/** Resultado do cadastro, usado pelo controller para montar a resposta. */
record RegisterResult(Long id, String fullName, String document, String email, UserType type) {
}
