package com.cinx.learning.certificate.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ApproveCertificatesRequest(
        @NotEmpty @Size(max = 100) List<@NotBlank String> requestIds
) {
}
