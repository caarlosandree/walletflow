package br.com.api.walletflow.user;

/** Lançada quando um usuário referenciado não existe. */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("Usuário não encontrado: " + id);
    }
}
