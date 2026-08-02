package com.bsplay.room.domain.exception;

public class RoomDomainException extends RuntimeException {
    private final String code;

    public RoomDomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}
