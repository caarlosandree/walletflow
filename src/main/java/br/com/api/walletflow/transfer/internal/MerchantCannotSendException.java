package br.com.api.walletflow.transfer.internal;

/** Lançada quando um lojista tenta enviar dinheiro (lojista apenas recebe). */
class MerchantCannotSendException extends RuntimeException {

    MerchantCannotSendException(Long merchantId) {
        super("Lojista não pode enviar transferências: " + merchantId);
    }
}
