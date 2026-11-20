package com.cinx.commerce.order.api.mapper;

import com.cinx.commerce.order.api.dto.CheckoutRequest;
import com.cinx.commerce.order.api.dto.OrderResponse;
import com.cinx.commerce.order.application.command.CheckoutCommand;
import com.cinx.commerce.order.application.query.OrderView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    OrderResponse toDto(OrderView order);

    CheckoutCommand toCommand(CheckoutRequest request);
}
