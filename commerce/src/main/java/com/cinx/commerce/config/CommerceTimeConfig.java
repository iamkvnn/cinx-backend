package com.cinx.commerce.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import java.time.*;

@Configuration
public class CommerceTimeConfig {
    @Bean
    Clock commerceClock(@Value("${commerce.time-zone:UTC}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
