package br.com.api.walletflow.user.internal;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByDocument(String document);

    boolean existsByEmail(String email);
}
