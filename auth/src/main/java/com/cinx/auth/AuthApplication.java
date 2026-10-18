package com.cinx.auth;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.cinx.auth", "com.cinx.common"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.cinx")
@OpenAPIDefinition(
		servers = {
				@Server(url = "${gateway.url}", description = "API Gateway")
		}
)
public class AuthApplication {

	public static void main(String[] args) {
		// business time is UTC everywhere (the database and the clocks); a host's own zone must not leak into rows
		java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
		SpringApplication.run(AuthApplication.class, args);
	}

}
