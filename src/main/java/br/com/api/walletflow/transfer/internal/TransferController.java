package br.com.api.walletflow.transfer.internal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfer")
@RequiredArgsConstructor
class TransferController {

    private final TransferService transferService;

    @PostMapping
    ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        TransferResult result = transferService.transfer(
                new TransferCommand(request.payer(), request.payee(), request.value(), idempotencyKey));

        return ResponseEntity.ok(new TransferResponse(
                result.id(), result.payerId(), result.payeeId(), result.amount()));
    }
}
