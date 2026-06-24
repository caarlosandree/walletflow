package br.com.api.walletflow.wallet.internal;

import br.com.api.walletflow.shared.Money;
import br.com.api.walletflow.wallet.InsufficientFundsException;
import br.com.api.walletflow.wallet.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ApplicationModuleTest
@Transactional
class WalletServiceTest {

    @Autowired
    private WalletService walletService;

    @Test
    void transferenciaMoveSaldoDeFormaAtomica() {
        walletService.deposit(1L, Money.of("100.00"));

        walletService.transfer(1L, 2L, Money.of("30.00"));

        assertEquals(Money.of("70.00"), walletService.balanceOf(1L));
        assertEquals(Money.of("30.00"), walletService.balanceOf(2L));
    }

    @Test
    void saldoInsuficienteLancaExcecao() {
        walletService.deposit(1L, Money.of("10.00"));

        assertThrows(InsufficientFundsException.class,
                () -> walletService.transfer(1L, 2L, Money.of("50.00")));
    }

    @Test
    void carteiraInexistenteTemSaldoZero() {
        assertEquals(Money.ZERO, walletService.balanceOf(999L));
    }
}
