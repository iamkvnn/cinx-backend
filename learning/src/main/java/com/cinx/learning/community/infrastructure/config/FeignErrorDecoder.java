package com.cinx.learning.community.infrastructure.config;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.NotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.stereotype.Component;

@Component
public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {

        return switch (response.status()) {
            case 400 -> new BadRequestException("Bad request from service");
            case 404 -> new NotFoundException("Resource not found");
            case 500 -> new RuntimeException("Internal server error");
            default -> new RuntimeException("Feign error");
        };
    }
}
