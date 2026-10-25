package com.cinx.learning;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

@OpenAPIDefinition(
                servers = {
                                @Server(url = "${gateway.url}", description = "API Gateway")
                }
)
@SpringBootApplication(
                scanBasePackages = {"com.cinx.learning", "com.cinx.common"},
                nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.cinx")
public class LearningApplication {

	public static void main(String[] args) {
		// business time is UTC everywhere (the database and the clocks); a host's own zone must not leak into rows
		java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
		SpringApplication.run(LearningApplication.class, args);
	}

}
