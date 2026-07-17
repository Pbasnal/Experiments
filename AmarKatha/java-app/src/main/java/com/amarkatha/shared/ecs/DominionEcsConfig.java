package com.amarkatha.shared.ecs;

import dev.dominion.ecs.api.Dominion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DominionEcsConfig {

    @Bean(destroyMethod = "close")
    public Dominion catalogDominion() {
        return Dominion.create();
    }
}
