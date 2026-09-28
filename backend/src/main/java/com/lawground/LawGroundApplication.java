package com.lawground;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LawGroundApplication {
    public static void main(String[] args) {
        SpringApplication.run(LawGroundApplication.class, args);
    }
}
