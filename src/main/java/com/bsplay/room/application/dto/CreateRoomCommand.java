package com.bsplay.room.application.dto;

public record CreateRoomCommand(String roomName, String hostName, int maxPlayers) {}
