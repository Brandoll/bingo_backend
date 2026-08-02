package com.bsplay.room.application.exception;

public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(String code) {
        super("No existe una sala con el código " + code + ".");
    }
}
