package com.cinx.commerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// com.cinx.common carries the shared exception handler that renders domain errors as problem+json
@SpringBootApplication(scanBasePackages = {"com.cinx.commerce", "com.cinx.common"})
public class CommerceApplication {
    public static void main(String[] args) {
        // business time is UTC everywhere (the database and the clocks); a host's own zone must not leak into rows
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
        SpringApplication.run(CommerceApplication.class, args);
    }
}
