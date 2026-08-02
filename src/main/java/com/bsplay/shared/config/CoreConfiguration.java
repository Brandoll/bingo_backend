package com.bsplay.shared.config;

import com.bsplay.room.domain.service.RoomCodeGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class CoreConfiguration {
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean RoomCodeGenerator roomCodeGenerator() { return new RoomCodeGenerator(); }
}
