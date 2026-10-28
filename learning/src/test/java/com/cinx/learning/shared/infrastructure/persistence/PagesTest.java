package com.cinx.learning.shared.infrastructure.persistence;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagesTest {
    @Test
    void thePageNumberIsOneBasedInTheApplicationAndZeroBasedInSpringData() {
        Pageable pageable = Pages.pageable(new PageQuery(3, 20, null));

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().isUnsorted()).isTrue();
    }

    @Test
    void theSortParameterKeepsItsFieldsAndDirections() {
        Pageable pageable = Pages.pageable(new PageQuery(1, 10, "{\"createdAt\":\"DESC\",\"title\":\"ASC\"}"));

        assertThat(pageable.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getSort().getOrderFor("title").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void aBrokenSortParameterIsABadRequest() {
        assertThatThrownBy(() -> Pages.pageable(new PageQuery(1, 10, "not json")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void aRequestForEveryRowIsUnpaged() {
        assertThat(Pages.pageable(PageQuery.unpaged()).isUnpaged()).isTrue();
        assertThat(Pages.pageable(PageQuery.unpaged(), "rating").isUnpaged()).isTrue();
    }

    @Test
    void namedPropertiesSortNullsLastAndTheOthersKeepTheDefault() {
        Pageable pageable = Pages.pageable(new PageQuery(1, 10, "{\"rating\":\"DESC\",\"title\":\"ASC\"}"), "rating");

        assertThat(pageable.getSort().getOrderFor("rating").getNullHandling()).isEqualTo(Sort.NullHandling.NULLS_LAST);
        assertThat(pageable.getSort().getOrderFor("title").getNullHandling()).isEqualTo(Sort.NullHandling.NATIVE);
    }

    @Test
    void aPageOfRowsBecomesAPageOfResultsWithTheRequestedNumbers() {
        Page<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 5);

        PageResult<String> result = Pages.result(page, new PageQuery(2, 2, null), String::toUpperCase);

        assertThat(result.content()).containsExactly("A", "B");
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(2);
        // 5 rows in pages of 2
        assertThat(result.totalElements()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    void anUnpagedResultCarriesEveryRowAsOnePage() {
        Page<String> page = new PageImpl<>(List.of("a", "b", "c"));

        PageResult<String> result = Pages.result(page, PageQuery.unpaged(), value -> value);

        assertThat(result.content()).hasSize(3);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(3);
    }
}
