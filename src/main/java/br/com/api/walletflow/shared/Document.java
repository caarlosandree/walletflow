package br.com.api.walletflow.shared;

/**
 * Documento de identificação: CPF (pessoa física) ou CNPJ (pessoa jurídica).
 *
 * <p>Tipo selado para permitir <em>pattern matching</em> exaustivo nas regras
 * que dependem de PF vs PJ. A factory {@link #of(String)} decide o tipo pelo
 * número de dígitos; cada subtipo valida os dígitos verificadores no construtor.
 */
public sealed interface Document permits Cpf, Cnpj {

    /** Apenas os dígitos, sem máscara. */
    String value();

    static Document of(String raw) {
        String digits = Documents.digitsOnly(raw);
        return switch (digits.length()) {
            case 11 -> new Cpf(digits);
            case 14 -> new Cnpj(digits);
            default -> throw new IllegalArgumentException(
                    "Documento deve ter 11 (CPF) ou 14 (CNPJ) dígitos: " + digits);
        };
    }
}
