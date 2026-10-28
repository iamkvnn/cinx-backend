package com.cinx.learning.shared.infrastructure.persistence;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.common.mapper.SortConverter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.function.Function;

/** Turns the application's page request into Spring Data's and a Spring Data page into the application's. */
public final class Pages {
    private Pages() {
    }

    public static Pageable pageable(PageQuery query) {
        if (query.isUnpaged()) {
            return Pageable.unpaged();
        }
        return PageRequest.of(query.page() - 1, query.size(), SortConverter.toSort(query.sort()));
    }

    /** Like {@link #pageable(PageQuery)}, but rows where one of the named properties is null come last in that sort. */
    public static Pageable pageable(PageQuery query, String... nullsLast) {
        if (query.isUnpaged()) {
            return Pageable.unpaged();
        }
        java.util.Set<String> last = java.util.Set.of(nullsLast);
        Sort sort = SortConverter.toSort(query.sort()).stream()
                .map(order -> last.contains(order.getProperty()) ? order.with(Sort.NullHandling.NULLS_LAST) : order)
                .collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(), Sort::by));
        return PageRequest.of(query.page() - 1, query.size(), sort);
    }

    public static <E, D> PageResult<D> result(Page<E> page, PageQuery query, Function<E, D> mapper) {
        var content = page.getContent().stream().map(mapper).toList();
        if (query.isUnpaged()) {
            return new PageResult<>(content, 1, content.size(), content.size());
        }
        return new PageResult<>(content, query.page(), query.size(), page.getTotalElements());
    }
}
