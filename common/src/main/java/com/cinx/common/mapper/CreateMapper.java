package com.cinx.common.mapper;

public interface CreateMapper<M, C> {
    M toModel(C createDto);
}

