package com.example.experienceplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ExperiencePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExperiencePlatformApplication.class, args);
    }

}
