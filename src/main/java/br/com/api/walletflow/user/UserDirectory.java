package br.com.api.walletflow.user;

/**
 * Provided interface do módulo {@code user}: consulta read-only de identidade
 * para outros módulos (ex.: {@code transfer} valida pagador/recebedor).
 */
public interface UserDirectory {

    /**
     * @throws UserNotFoundException se não houver usuário com o id informado.
     */
    Account findById(Long id);

    boolean existsById(Long id);
}
