package br.com.api.walletflow.user.internal;

/** Lançada quando CPF/CNPJ ou e-mail já estão cadastrados. */
class DuplicateUserException extends RuntimeException {

    DuplicateUserException(String message) {
        super(message);
    }
}
