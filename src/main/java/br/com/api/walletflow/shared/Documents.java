package br.com.api.walletflow.shared;

/** Utilitários internos de normalização e validação de documentos. */
final class Documents {

    private Documents() {
    }

    static String digitsOnly(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("O documento não pode ser nulo.");
        }
        return raw.replaceAll("\\D", "");
    }

    /** Todos os caracteres iguais (ex.: 00000000000) passam no cálculo mas são inválidos. */
    static boolean allDigitsEqual(String digits) {
        return digits.chars().distinct().count() == 1;
    }

    /** Dígito verificador módulo 11 a partir da soma ponderada. */
    static int checkDigit(int weightedSum) {
        int mod = weightedSum % 11;
        return mod < 2 ? 0 : 11 - mod;
    }

    static int digitAt(String digits, int index) {
        return digits.charAt(index) - '0';
    }
}
