package br.com.api.walletflow.transfer.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * Implementação HTTP do autorizador. Fail-closed: qualquer erro/indisponibilidade
 * resulta em "não autorizado" — nunca aprova um pagamento em caso de dúvida.
 */
@Component
@RequiredArgsConstructor
class HttpAuthorizationClient implements AuthorizationClient {

    private final AuthorizationApi api;

    @Override
    public boolean isAuthorized() {
        try {
            AuthorizationApi.AuthorizationResponse response = api.authorize();
            return response != null
                    && response.data() != null
                    && response.data().authorization();
        } catch (RestClientException ex) {
            return false;
        }
    }
}
