package com.cinx.user;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@OpenAPIDefinition(
		servers = {
				@Server(url = "${gateway.url}", description = "API Gateway")
		}
)
@SpringBootApplication(scanBasePackages = {"com.cinx.user", "com.cinx.common"})
@EnableDiscoveryClient
public class UserApplication {

	public static void main(String[] args) {
		// business time is UTC everywhere (the database and the clocks); a host's own zone must not leak into rows
		java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
		SpringApplication.run(UserApplication.class, args);
	}

}
