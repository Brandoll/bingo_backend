package com.bsplay.game.domain.exception;

public class GameDomainException extends RuntimeException {
    private final String code;

    public GameDomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
