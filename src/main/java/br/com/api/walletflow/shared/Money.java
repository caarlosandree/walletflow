package br.com.api.walletflow.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Valor monetário não-negativo, em escala fixa de 2 casas decimais.
 *
 * <p>O construtor compacto garante a invariante (nunca negativo, sempre escala 2),
 * tornando impossível existir um {@code Money} inválido. {@link #debit(Money)}
 * rejeita resultado negativo, eliminando a classe de bug "saldo negativo".
 */
public record Money(BigDecimal amount) implements Comparable<Money> {

    public static final Money ZERO = Money.of("0");

    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("O valor monetário não pode ser nulo.");
        }
        amount = amount.setScale(2, RoundingMode.HALF_EVEN);
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("O valor monetário não pode ser negativo: " + amount);
        }
    }

    public static Money of(BigDecimal value) {
        return new Money(value);
    }

    public static Money of(String value) {
        return new Money(new BigDecimal(value));
    }

    /** Soma (crédito). */
    public Money credit(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    /** Subtração (débito). Lança {@link IllegalArgumentException} se o resultado for negativo. */
    public Money debit(Money other) {
        return new Money(this.amount.subtract(other.amount));
    }

    public boolean isLessThan(Money other) {
        return this.amount.compareTo(other.amount) < 0;
    }

    @Override
    public int compareTo(Money other) {
        return this.amount.compareTo(other.amount);
    }
}
