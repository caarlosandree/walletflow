package br.com.api.walletflow.shared;

/** CNPJ: 14 dígitos com dois dígitos verificadores (módulo 11). */
public record Cnpj(String value) implements Document {

    private static final int[] WEIGHTS_DV1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_DV2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    public Cnpj {
        value = Documents.digitsOnly(value);
        if (!isValid(value)) {
            throw new IllegalArgumentException("CNPJ inválido: " + value);
        }
    }

    private static boolean isValid(String cnpj) {
        if (cnpj.length() != 14 || Documents.allDigitsEqual(cnpj)) {
            return false;
        }
        int dv1 = Documents.checkDigit(weightedSum(cnpj, WEIGHTS_DV1));
        int dv2 = Documents.checkDigit(weightedSum(cnpj, WEIGHTS_DV2));
        return dv1 == Documents.digitAt(cnpj, 12) && dv2 == Documents.digitAt(cnpj, 13);
    }

    private static int weightedSum(String cnpj, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += Documents.digitAt(cnpj, i) * weights[i];
        }
        return sum;
    }
}
