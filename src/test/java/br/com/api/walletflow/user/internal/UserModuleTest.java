package br.com.api.walletflow.user.internal;

import br.com.api.walletflow.user.Account;
import br.com.api.walletflow.user.CommonUser;
import br.com.api.walletflow.user.Merchant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@ApplicationModuleTest
@Transactional
class UserModuleTest {

    @Autowired
    private UserService userService;

    @Test
    void registraEConsultaComoCommonUser() {
        RegisterResult result = userService.register(new RegisterUserRequest(
                "João Comum", "529.982.247-25", "joao@example.com", "secret", UserType.COMMON));

        Account account = userService.findById(result.id());

        assertInstanceOf(CommonUser.class, account);
        assertEquals("52998224725", account.document().value());
    }

    @Test
    void lojistaMapeiaParaMerchant() {
        RegisterResult result = userService.register(new RegisterUserRequest(
                "Loja Exemplo", "11.222.333/0001-81", "loja@example.com", "secret", UserType.MERCHANT));

        assertInstanceOf(Merchant.class, userService.findById(result.id()));
    }
}
