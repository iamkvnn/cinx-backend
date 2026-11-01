package com.cinx.learning.certificate.api.mapper;

import com.cinx.learning.certificate.api.dto.request.CertificateConditionsRequest;
import com.cinx.learning.certificate.api.dto.response.CertificateConditionsResponse;
import com.cinx.learning.certificate.api.dto.response.CertificateRequestResponse;
import com.cinx.learning.certificate.application.command.CertificateConditionsCommand;
import com.cinx.learning.certificate.application.query.CertificateConditionsView;
import com.cinx.learning.certificate.application.query.CertificateRequestView;
import org.mapstruct.Mapper;

/** Converts the api's transport types to the application's commands and the application's views to transport types. */
@Mapper(componentModel = "spring")
public interface CertificateApiMapper {
    CertificateConditionsCommand toCommand(CertificateConditionsRequest request);
    CertificateConditionsResponse toResponse(CertificateConditionsView view);
    CertificateRequestResponse toResponse(CertificateRequestView view);
    com.cinx.learning.certificate.api.dto.response.CertificateConditionsResponse.Saved toResponse(com.cinx.learning.certificate.application.query.CertificateConditionsView.Saved view);
    com.cinx.learning.certificate.api.dto.response.CertificateConditionsResponse.BatchResult toResponse(com.cinx.learning.certificate.application.query.CertificateConditionsView.BatchResult view);
    com.cinx.learning.certificate.api.dto.response.CertificateConditionsResponse.Result toResponse(com.cinx.learning.certificate.application.query.CertificateConditionsView.Result view);
    com.cinx.learning.certificate.api.dto.response.CertificateRequestResponse.CertificateChecks toResponse(com.cinx.learning.certificate.application.query.CertificateRequestView.CertificateChecks view);
}
