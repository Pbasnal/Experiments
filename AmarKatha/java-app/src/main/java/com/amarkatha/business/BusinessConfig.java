package com.amarkatha.business;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusinessConfig {

    @Bean
    HomeDiscoveryPolicy homeDiscoveryPolicy() {
        return new HomeDiscoveryPolicy();
    }

    @Bean
    ReadingEntryPolicy readingEntryPolicy() {
        return new ReadingEntryPolicy();
    }

    @Bean
    ExperienceSourcePolicy experienceSourcePolicy() {
        return new ExperienceSourcePolicy();
    }
}
