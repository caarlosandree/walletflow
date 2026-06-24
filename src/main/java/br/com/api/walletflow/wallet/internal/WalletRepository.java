package br.com.api.walletflow.wallet.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface WalletRepository extends JpaRepository<WalletEntity, Long> {

    Optional<WalletEntity> findByUserId(Long userId);
}
