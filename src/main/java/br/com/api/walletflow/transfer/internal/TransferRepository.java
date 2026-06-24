package br.com.api.walletflow.transfer.internal;

import org.springframework.data.jpa.repository.JpaRepository;

interface TransferRepository extends JpaRepository<TransferEntity, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);
}
