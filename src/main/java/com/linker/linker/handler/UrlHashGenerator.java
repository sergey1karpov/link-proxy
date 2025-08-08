package com.linker.linker.handler;

import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.UUID;

@Service
public class UrlHashGenerator {
    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public static String generateBase62Id() {
        UUID uuid = UUID.randomUUID();
        BigInteger number = new BigInteger(uuid.toString().replace("-", ""), 16);

        StringBuilder result = new StringBuilder();
        while (number.compareTo(BigInteger.ZERO) > 0 && result.length() < 12) {
            BigInteger[] divmod = number.divideAndRemainder(BigInteger.valueOf(62));
            result.append(BASE62.charAt(divmod[1].intValue()));
            number = divmod[0];
        }

        while (result.length() < 12) {
            result.append(BASE62.charAt((int) (Math.random() * 62)));
        }

        return result.reverse().toString();
    }
}
