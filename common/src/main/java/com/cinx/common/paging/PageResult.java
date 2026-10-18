package com.cinx.common.paging;

import java.util.List;
import java.util.function.Function;

/**
 * One page of rows with the numbers an API needs to describe it.
 *
 * @param page one-based page number
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements) {
    public int totalPages() {
        return size == 0 ? 1 : (int) Math.ceil((double) totalElements / size);
    }

    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        return new PageResult<>(content.stream().<R>map(mapper).toList(), page, size, totalElements);
    }
}
