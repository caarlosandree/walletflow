package br.com.api.walletflow.wallet.internal;

import br.com.api.walletflow.shared.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class WalletEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private BigDecimal balance;

    @Version
    private Long version;

    WalletEntity(Long userId) {
        this.userId = userId;
        this.balance = Money.ZERO.amount();
    }

    Money balance() {
        return Money.of(balance);
    }

    void credit(Money amount) {
        this.balance = balance().credit(amount).amount();
    }

    void debit(Money amount) {
        this.balance = balance().debit(amount).amount();
    }
}
