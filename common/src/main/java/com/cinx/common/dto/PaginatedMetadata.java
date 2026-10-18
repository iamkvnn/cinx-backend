package com.cinx.common.dto;

public record PaginatedMetadata (int page, int limit, long totalElements, int totalPages) {
}
