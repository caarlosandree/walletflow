package br.com.api.walletflow.transfer.internal;

/** Abstração da decisão do autorizador (facilita substituição em testes). */
interface AuthorizationClient {

    boolean isAuthorized();
}
