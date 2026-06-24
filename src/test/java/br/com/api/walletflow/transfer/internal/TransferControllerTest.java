package br.com.api.walletflow.transfer.internal;

import br.com.api.walletflow.shared.Money;
import br.com.api.walletflow.transfer.TransferCompleted;
import br.com.api.walletflow.wallet.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@RecordApplicationEvents
class TransferControllerTest {

    private static final String CPF_A = "111.444.777-35";
    private static final String CPF_B = "529.982.247-25";
    private static final String CNPJ = "11.222.333/0001-81";

    @TestConfiguration
    static class StubConfig {
        @Bean
        @Primary
        StubAuthorizationClient stubAuthorizationClient() {
            return new StubAuthorizationClient();
        }
    }

    static class StubAuthorizationClient implements AuthorizationClient {
        volatile boolean authorized = true;

        @Override
        public boolean isAuthorized() {
            return authorized;
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private WalletService walletService;
    @Autowired
    private StubAuthorizationClient authorizer;
    @Autowired
    private ApplicationEvents events;

    @BeforeEach
    void resetAuthorizer() {
        authorizer.authorized = true;
    }

    @Test
    void transferenciaFelizMoveSaldoEPublicaEvento() throws Exception {
        Long payer = createUser("payer@example.com", CPF_A, "COMMON");
        Long payee = createUser("payee@example.com", CPF_B, "COMMON");
        walletService.deposit(payer, Money.of("100.00"));

        transfer("30.00", payer, payee, null).andExpect(status().isOk());

        assertEquals(Money.of("70.00"), walletService.balanceOf(payer));
        assertEquals(Money.of("30.00"), walletService.balanceOf(payee));
        assertEquals(1, events.stream(TransferCompleted.class).count());
    }

    @Test
    void autorizadorRecusadoFazRollbackComStatus403() throws Exception {
        Long payer = createUser("payer@example.com", CPF_A, "COMMON");
        Long payee = createUser("payee@example.com", CPF_B, "COMMON");
        walletService.deposit(payer, Money.of("100.00"));
        authorizer.authorized = false;

        transfer("30.00", payer, payee, null).andExpect(status().isForbidden());

        assertEquals(Money.of("100.00"), walletService.balanceOf(payer));
        assertEquals(0, events.stream(TransferCompleted.class).count());
    }

    @Test
    void lojistaNaoEnviaComStatus422() throws Exception {
        Long merchant = createUser("merchant@example.com", CNPJ, "MERCHANT");
        Long payee = createUser("payee@example.com", CPF_A, "COMMON");
        walletService.deposit(merchant, Money.of("100.00"));

        transfer("30.00", merchant, payee, null).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void saldoInsuficienteComStatus422() throws Exception {
        Long payer = createUser("payer@example.com", CPF_A, "COMMON");
        Long payee = createUser("payee@example.com", CPF_B, "COMMON");
        walletService.deposit(payer, Money.of("10.00"));

        transfer("50.00", payer, payee, null).andExpect(status().isUnprocessableEntity());
        assertEquals(Money.of("10.00"), walletService.balanceOf(payer));
    }

    @Test
    void idempotenciaRejeitaDebitoDuplicadoComStatus409() throws Exception {
        Long payer = createUser("payer@example.com", CPF_A, "COMMON");
        Long payee = createUser("payee@example.com", CPF_B, "COMMON");
        walletService.deposit(payer, Money.of("100.00"));

        transfer("30.00", payer, payee, "key-123").andExpect(status().isOk());
        transfer("30.00", payer, payee, "key-123").andExpect(status().isConflict());

        assertEquals(Money.of("70.00"), walletService.balanceOf(payer));
    }

    private Long createUser(String email, String document, String type) throws Exception {
        String location = mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Teste","document":"%s","email":"%s","password":"secret","type":"%s"}
                                """.formatted(document, email, type)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private org.springframework.test.web.servlet.ResultActions transfer(
            String value, Long payer, Long payee, String idempotencyKey) throws Exception {
        MockHttpServletRequestBuilder request = post("/transfer").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"value":%s,"payer":%d,"payee":%d}
                        """.formatted(value, payer, payee));
        if (idempotencyKey != null) {
            request = request.header("Idempotency-Key", idempotencyKey);
        }
        return mockMvc.perform(request);
    }
}
