package com.openbankproject.hydra.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableCaching
// Required by HealthCheckRegistry: /status reads results a background sweep produced, so without
// scheduling every service would sit at "unknown" forever.
@EnableScheduling
@SpringBootApplication
@ComponentScan(basePackages = "com.openbankproject")  // Ensure this package is scanned
public class ObpHydraAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(ObpHydraAuthApplication.class, args);
    }
}
