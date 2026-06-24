package br.com.api.walletflow.transfer.internal;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Cliente declarativo do autorizador externo (GET /authorize). */
@HttpExchange(accept = "application/json")
interface AuthorizationApi {

    @GetExchange("/authorize")
    AuthorizationResponse authorize();

    record AuthorizationResponse(String status, Data data) {
        record Data(boolean authorization) {
        }
    }
}
