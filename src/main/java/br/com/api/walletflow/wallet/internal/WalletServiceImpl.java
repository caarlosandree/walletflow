package br.com.api.walletflow.wallet.internal;

import br.com.api.walletflow.shared.Money;
import br.com.api.walletflow.wallet.InsufficientFundsException;
import br.com.api.walletflow.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class WalletServiceImpl implements WalletService {

    private final WalletRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Money balanceOf(Long userId) {
        return repository.findByUserId(userId)
                .map(WalletEntity::balance)
                .orElse(Money.ZERO);
    }

    @Override
    @Transactional
    public void deposit(Long userId, Money amount) {
        WalletEntity wallet = getOrCreate(userId);
        wallet.credit(amount);
        repository.save(wallet);
    }

    @Override
    @Transactional
    public void transfer(Long fromUserId, Long toUserId, Money amount) {
        if (fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("Não é possível transferir para a mesma carteira.");
        }

        WalletEntity payer = getOrCreate(fromUserId);
        WalletEntity payee = getOrCreate(toUserId);

        if (payer.balance().isLessThan(amount)) {
            throw new InsufficientFundsException(fromUserId, payer.balance(), amount);
        }

        payer.debit(amount);
        payee.credit(amount);
        repository.save(payer);
        repository.save(payee);
    }

    private WalletEntity getOrCreate(Long userId) {
        return repository.findByUserId(userId)
                .orElseGet(() -> repository.save(new WalletEntity(userId)));
    }
}
