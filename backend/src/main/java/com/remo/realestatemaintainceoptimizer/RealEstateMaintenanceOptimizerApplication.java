package com.remo.realestatemaintainceoptimizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// entry point that boots the Spring application context
@SpringBootApplication
public class RealEstateMaintenanceOptimizerApplication {

    // starts the embedded server and wires all Spring beans
    public static void main(String[] args) {
        SpringApplication.run(RealEstateMaintenanceOptimizerApplication.class, args);
    }
}
