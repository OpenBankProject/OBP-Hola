package com.openbankproject.hydra.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

@EnableCaching
@SpringBootApplication
@ComponentScan(basePackages = "com.openbankproject")  // Ensure this package is scanned
public class ObpHydraAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(ObpHydraAuthApplication.class, args);
    }
}
