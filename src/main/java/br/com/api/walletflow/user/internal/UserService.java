package br.com.api.walletflow.user.internal;

import br.com.api.walletflow.shared.Document;
import br.com.api.walletflow.user.Account;
import br.com.api.walletflow.user.CommonUser;
import br.com.api.walletflow.user.Merchant;
import br.com.api.walletflow.user.UserDirectory;
import br.com.api.walletflow.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class UserService implements UserDirectory {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    RegisterResult register(RegisterUserRequest request) {
        Document document = Document.of(request.document());
        String digits = document.value();

        if (repository.existsByDocument(digits)) {
            throw new DuplicateUserException("CPF/CNPJ já cadastrado: " + digits);
        }
        if (repository.existsByEmail(request.email())) {
            throw new DuplicateUserException("E-mail já cadastrado: " + request.email());
        }

        UserEntity saved = repository.save(new UserEntity(
                request.fullName(),
                digits,
                request.email(),
                passwordEncoder.encode(request.password()),
                request.type()));

        return new RegisterResult(saved.getId(), saved.getFullName(), saved.getDocument(),
                saved.getEmail(), saved.getType());
    }

    @Override
    @Transactional(readOnly = true)
    public Account findById(Long id) {
        return repository.findById(id)
                .map(UserService::toAccount)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    private static Account toAccount(UserEntity entity) {
        Document document = Document.of(entity.getDocument());
        return entity.getType() == UserType.MERCHANT
                ? new Merchant(entity.getId(), document)
                : new CommonUser(entity.getId(), document);
    }
}
