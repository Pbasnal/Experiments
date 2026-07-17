package com.amarkatha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AmarKathaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmarKathaApplication.class, args);
    }
}
