package com.cinx.common.mapper;

public interface BaseMapper<M, D> {
    D toDto(M model);
}
