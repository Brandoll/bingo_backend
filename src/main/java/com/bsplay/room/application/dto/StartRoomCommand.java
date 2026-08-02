package com.bsplay.room.application.dto;

import java.util.UUID;

public record StartRoomCommand(String roomCode, UUID roomId, UUID memberId) {}
