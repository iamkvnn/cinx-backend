package com.cinx.common.mapper;

import com.cinx.common.dto.PaginatedApiResponse;
import com.cinx.common.dto.PaginatedMetadata;
import com.cinx.common.paging.PageResult;
import org.springframework.data.domain.Page;

public class PaginationWrapper {
    public static <T> PaginatedApiResponse<T> wrap(Page<T> page) {
        return new PaginatedApiResponse<>(
                true,
                "Data fetched successfully",
                page.getContent(),
                new PaginatedMetadata(
                        page.getNumber() + 1,
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages()
                )
        );
    }

    public static <T> PaginatedApiResponse<T> wrap(PageResult<T> page) {
        return new PaginatedApiResponse<>(
                true,
                "Data fetched successfully",
                page.content(),
                new PaginatedMetadata(page.page(), page.size(), page.totalElements(), page.totalPages())
        );
    }
}
