package br.com.api.walletflow.wallet.internal;

import br.com.api.walletflow.shared.Money;
import br.com.api.walletflow.wallet.InsufficientFundsException;
import br.com.api.walletflow.wallet.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Prova que o lock otimista (@Version) impede saldo negativo/lost-update sob
 * concorrência e que o retry resolve os conflitos — exatamente o retry que a
 * orquestração de transferência (T4) aplicará em produção.
 */
@SpringBootTest
class WalletConcurrencyTest {

    private static final Long PAYER = 9001L;
    private static final Long PAYEE = 9002L;

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository repository;

    @BeforeEach
    void reset() {
        repository.findByUserId(PAYER).ifPresent(repository::delete);
        repository.findByUserId(PAYEE).ifPresent(repository::delete);
    }

    @Test
    void transferenciasConcorrentesNaoGeramSaldoNegativo() throws InterruptedException {
        walletService.deposit(PAYER, Money.of("100.00"));
        walletService.deposit(PAYEE, Money.ZERO); // pré-cria a carteira do recebedor (evita corrida de criação)

        int threads = 10;
        Money each = Money.of("10.00");
        ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger succeeded = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    transferWithRetry(each);
                    succeeded.incrementAndGet();
                } catch (InsufficientFundsException ignored) {
                    // esperado se o saldo acabar
                } finally {
                    done.countDown();
                }
            });
        }

        done.await(15, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(10, succeeded.get());
        assertEquals(Money.of("0.00"), walletService.balanceOf(PAYER));
        assertEquals(Money.of("100.00"), walletService.balanceOf(PAYEE));
    }

    private void transferWithRetry(Money amount) {
        int attempts = 0;
        while (true) {
            try {
                walletService.transfer(PAYER, PAYEE, amount);
                return;
            } catch (OptimisticLockingFailureException ex) {
                if (++attempts > 50) {
                    throw ex;
                }
            }
        }
    }
}
