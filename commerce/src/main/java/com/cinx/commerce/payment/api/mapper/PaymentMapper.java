package com.cinx.commerce.payment.api.mapper;

import com.cinx.commerce.payment.api.dto.PaymentLinkResponse;
import com.cinx.commerce.payment.application.query.PaymentLink;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    PaymentLinkResponse toResponse(PaymentLink link);
}
