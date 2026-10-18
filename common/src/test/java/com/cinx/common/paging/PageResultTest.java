package com.cinx.common.paging;

import com.cinx.common.dto.PaginatedApiResponse;
import com.cinx.common.mapper.PaginationWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResultTest {
    @Test
    void totalPagesRoundsUp() {
        // 21 rows at 10 per page need 3 pages
        assertThat(new PageResult<>(List.of(), 1, 10, 21).totalPages()).isEqualTo(3);
        assertThat(new PageResult<>(List.of(), 1, 10, 20).totalPages()).isEqualTo(2);
        assertThat(new PageResult<>(List.of(), 1, 10, 0).totalPages()).isZero();
    }

    @Test
    void aZeroSizePageCountsAsOnePageLikeSpringDoes() {
        assertThat(new PageResult<>(List.of(), 1, 0, 5).totalPages()).isEqualTo(1);
    }

    @Test
    void mapKeepsThePositionAndTheTotals() {
        PageResult<Integer> mapped = new PageResult<>(List.of("a", "bb"), 3, 2, 11).map(String::length);

        assertThat(mapped.content()).containsExactly(1, 2);
        assertThat(mapped.page()).isEqualTo(3);
        assertThat(mapped.size()).isEqualTo(2);
        assertThat(mapped.totalElements()).isEqualTo(11);
    }

    @Test
    void wrapDescribesTheSamePageAsSpringsPageDoes() {
        PaginatedApiResponse<String> fromSpring = PaginationWrapper.wrap(
                new PageImpl<>(List.of("a", "b"), PageRequest.of(2, 2), 7));
        PaginatedApiResponse<String> fromResult = PaginationWrapper.wrap(new PageResult<>(List.of("a", "b"), 3, 2, 7));

        assertThat(fromResult).usingRecursiveComparison().isEqualTo(fromSpring);
        assertThat(fromResult.meta().page()).isEqualTo(3);
        assertThat(fromResult.meta().totalPages()).isEqualTo(4);
    }
}
