package br.com.api.walletflow.transfer.internal;

/** Lançada quando o autorizador externo recusa (ou está indisponível). */
class TransferNotAuthorizedException extends RuntimeException {

    TransferNotAuthorizedException() {
        super("Transferência não autorizada pelo serviço autorizador.");
    }
}
