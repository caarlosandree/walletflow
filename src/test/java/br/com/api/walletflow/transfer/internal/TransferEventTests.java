package br.com.api.walletflow.transfer.internal;

import br.com.api.walletflow.shared.Document;
import br.com.api.walletflow.shared.Money;
import br.com.api.walletflow.transfer.TransferCompleted;
import br.com.api.walletflow.user.Account;
import br.com.api.walletflow.user.CommonUser;
import br.com.api.walletflow.user.UserDirectory;
import br.com.api.walletflow.wallet.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Valida o fluxo de evento de ponta a ponta: uma transferência bem-sucedida
 * publica {@link TransferCompleted}. Colaboradores de outros módulos são
 * substituídos por stubs (o módulo transfer roda isolado).
 */
@ApplicationModuleTest
class TransferEventTests {

    @TestConfiguration
    static class Stubs {

        @Bean
        UserDirectory userDirectory() {
            return new UserDirectory() {
                @Override
                public Account findById(Long id) {
                    return new CommonUser(id, Document.of(id == 1L ? "11144477735" : "52998224725"));
                }

                @Override
                public boolean existsById(Long id) {
                    return true;
                }
            };
        }

        @Bean
        WalletService walletService() {
            return new WalletService() {
                @Override
                public Money balanceOf(Long userId) {
                    return Money.ZERO;
                }

                @Override
                public void deposit(Long userId, Money amount) {
                }

                @Override
                public void transfer(Long fromUserId, Long toUserId, Money amount) {
                }
            };
        }

        @Bean
        @Primary
        AuthorizationClient authorizationClient() {
            return () -> true;
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private TransferService transferService;

    @Test
    void transferenciaPublicaTransferCompleted(Scenario scenario) {
        scenario.stimulate(() -> transferService.transfer(
                        new TransferCommand(1L, 2L, new BigDecimal("30.00"), null)))
                .andWaitForEventOfType(TransferCompleted.class)
                .toArriveAndVerify(event -> {
                    assertEquals(1L, event.payerId());
                    assertEquals(2L, event.payeeId());
                    assertEquals(new BigDecimal("30.00"), event.amount());
                });
    }
}
