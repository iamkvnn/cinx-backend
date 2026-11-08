package com.cinx.commerce.subscription.api.mapper;

import com.cinx.commerce.subscription.api.dto.CheckoutSessionResponse;
import com.cinx.commerce.subscription.api.dto.SubscriptionAdjustmentRequest;
import com.cinx.commerce.subscription.api.dto.SubscriptionResponse;
import com.cinx.commerce.subscription.application.command.AdjustSubscriptionCommand;
import com.cinx.commerce.subscription.application.query.CheckoutSessionView;
import com.cinx.commerce.subscription.application.query.SubscriptionView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {
    SubscriptionResponse toResponse(SubscriptionView view);

    CheckoutSessionResponse toResponse(CheckoutSessionView view);

    AdjustSubscriptionCommand toCommand(SubscriptionAdjustmentRequest request);
}
