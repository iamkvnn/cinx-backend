package com.cinx.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                        )
                        .addSchemas("FieldValidationError", fieldValidationErrorSchema())
                        .addSchemas("ProblemDetail", problemDetailSchema())
                )
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .info(new Info().title("Cinx API").version("v1"));
    }

    private Schema<?> problemDetailSchema() {
        return new ObjectSchema()
                .addProperty("type", new StringSchema().example("urn:cinx:problem:validation-failed"))
                .addProperty("title", new StringSchema().example("Validation failed"))
                .addProperty("status", new IntegerSchema().example(400))
                .addProperty("detail", new StringSchema().example("Validation failed"))
                .addProperty("instance", new StringSchema().example("/api/v1/courses"))
                .addProperty("code", new StringSchema().example("VALIDATION_FAILED"))
                .addProperty("timestamp", new StringSchema().example("2026-06-06T14:00:00Z"))
                .addProperty("traceId", new StringSchema().example("f47ac10b-58cc-4372-a567-0e02b2c3d479"))
                .addProperty("errors", new ArraySchema().items(new Schema<>().$ref("#/components/schemas/FieldValidationError")));
    }

    private Schema<?> fieldValidationErrorSchema() {
        return new ObjectSchema()
                .addProperty("field", new StringSchema().example("email"))
                .addProperty("message", new StringSchema().example("must be a well-formed email address"))
                .addProperty("rejectedValue", new ObjectSchema().nullable(true));
    }
}
