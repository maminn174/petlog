package com.petlog.dashboard;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class DashboardConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
