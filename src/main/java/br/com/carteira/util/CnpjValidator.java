package br.com.carteira.util;

import java.util.stream.IntStream;

public final class CnpjValidator {
    private CnpjValidator() {}

    public static boolean isValid(String raw) {
        if (raw == null) return false;
        String cnpj = raw.replaceAll("\\D", "");
        if (cnpj.length() != 14 || cnpj.chars().distinct().count() == 1) return false;
        return digit(cnpj.substring(0, 12), new int[]{5,4,3,2,9,8,7,6,5,4,3,2}) == cnpj.charAt(12) - '0'
                && digit(cnpj.substring(0, 13), new int[]{6,5,4,3,2,9,8,7,6,5,4,3,2}) == cnpj.charAt(13) - '0';
    }

    private static int digit(String base, int[] weights) {
        int sum = IntStream.range(0, weights.length)
                .map(i -> (base.charAt(i) - '0') * weights[i]).sum();
        int result = 11 - (sum % 11);
        return result >= 10 ? 0 : result;
    }
}
