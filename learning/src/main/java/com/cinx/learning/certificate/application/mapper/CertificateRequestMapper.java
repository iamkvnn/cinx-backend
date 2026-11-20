package com.cinx.learning.certificate.application.mapper;

import com.cinx.common.mapper.BaseMapper;
import com.cinx.learning.certificate.application.query.CertificateRequestView;
import com.cinx.learning.certificate.domain.CertificateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CertificateRequestMapper extends BaseMapper<CertificateRequest, CertificateRequestView> {
    /** The condition checks are computed per request by the service, never stored. */
    @Override
    @Mapping(target = "checks", ignore = true)
    CertificateRequestView toDto(CertificateRequest model);
}
