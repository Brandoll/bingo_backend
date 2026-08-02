package com.bsplay.room.application.mapper;

import com.bsplay.room.application.dto.MemberResponse;
import com.bsplay.room.application.dto.RoomResponse;
import com.bsplay.room.domain.model.Room;
import com.bsplay.room.domain.model.RoomMember;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoomResponseMapper {
    RoomResponse toResponse(Room room);
    MemberResponse toResponse(RoomMember member);
}
