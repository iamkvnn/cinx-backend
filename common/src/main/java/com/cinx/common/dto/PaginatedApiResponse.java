package com.cinx.common.dto;

import java.util.List;

public record PaginatedApiResponse<T>(boolean success, String message, List<T> data, PaginatedMetadata meta) {
}
