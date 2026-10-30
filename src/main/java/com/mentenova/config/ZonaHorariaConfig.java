package com.mentenova.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ZonaHorariaConfig {

    @Bean
    public Clock reloj(@Value("${app.timezone:America/Lima}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
