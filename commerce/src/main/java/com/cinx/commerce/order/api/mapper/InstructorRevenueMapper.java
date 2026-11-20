package com.cinx.commerce.order.api.mapper;

import com.cinx.commerce.order.api.dto.InstructorRevenueResponse;
import com.cinx.commerce.order.application.query.InstructorRevenue;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InstructorRevenueMapper {
    InstructorRevenueResponse toResponse(InstructorRevenue revenue);
}
