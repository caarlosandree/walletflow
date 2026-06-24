package br.com.api.walletflow.shared;

/** CPF: 11 dígitos com dois dígitos verificadores (módulo 11). */
public record Cpf(String value) implements Document {

    public Cpf {
        value = Documents.digitsOnly(value);
        if (!isValid(value)) {
            throw new IllegalArgumentException("CPF inválido: " + value);
        }
    }

    private static boolean isValid(String cpf) {
        if (cpf.length() != 11 || Documents.allDigitsEqual(cpf)) {
            return false;
        }
        int dv1 = Documents.checkDigit(weightedSum(cpf, 9, 10));
        int dv2 = Documents.checkDigit(weightedSum(cpf, 10, 11));
        return dv1 == Documents.digitAt(cpf, 9) && dv2 == Documents.digitAt(cpf, 10);
    }

    private static int weightedSum(String cpf, int length, int startWeight) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += Documents.digitAt(cpf, i) * (startWeight - i);
        }
        return sum;
    }
}
