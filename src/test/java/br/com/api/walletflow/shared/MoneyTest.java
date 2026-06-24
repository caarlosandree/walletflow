package br.com.api.walletflow.shared;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

    @Test
    void rejeitaValorNegativo() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(new BigDecimal("-1.00")));
    }

    @Test
    void rejeitaValorNulo() {
        assertThrows(IllegalArgumentException.class, () -> Money.of((BigDecimal) null));
    }

    @Test
    void normalizaEscalaParaDuasCasas() {
        assertEquals(new BigDecimal("10.00"), Money.of("10").amount());
    }

    @Test
    void creditSoma() {
        assertEquals(Money.of("30.00"), Money.of("10").credit(Money.of("20")));
    }

    @Test
    void debitSubtrai() {
        assertEquals(Money.of("5.00"), Money.of("10").debit(Money.of("5")));
    }

    @Test
    void debitRejeitaResultadoNegativo() {
        assertThrows(IllegalArgumentException.class, () -> Money.of("10").debit(Money.of("20")));
    }

    @Test
    void isLessThanCompara() {
        assertTrue(Money.of("5").isLessThan(Money.of("10")));
    }
}
