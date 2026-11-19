package com.cinx.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.cinx.reporting", "com.cinx.common"})
public class ReportingApplication {
    public static void main(String[] args) {
        // business time is UTC everywhere (the database and the clocks); a host's own zone must not leak into rows
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
        SpringApplication.run(ReportingApplication.class, args);
    }
}
