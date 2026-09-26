package com.pickview.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfiguration {
    @Bean
    public Clock applicationClock() { return Clock.systemUTC(); }
}
