package com.bsplay.room.domain.service;

import java.security.SecureRandom;

public final class RoomCodeGenerator {
    private static final char[] ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private final SecureRandom random = new SecureRandom();

    public String nextCode() {
        var code = new char[6];
        for (int index = 0; index < code.length; index++) {
            code[index] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(code);
    }
}
