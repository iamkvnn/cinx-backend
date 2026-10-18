package com.cinx.common.mapper;

import org.mapstruct.*;

public interface UpdateMapper<M, U> {
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void partialUpdate(@MappingTarget M m, U updateDto);
}
