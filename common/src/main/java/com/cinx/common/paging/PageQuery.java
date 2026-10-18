package com.cinx.common.paging;

/**
 * A request for one page, free of any persistence type so an application service and its query port can share it.
 *
 * @param page one-based page number
 * @param size rows per page; zero or less asks for every row
 * @param sort the caller's sort as the API receives it (a JSON object of field to direction), or null for the query's default
 */
public record PageQuery(int page, int size, String sort) {

    /** Every row, in the query's default order. */
    public static PageQuery unpaged() {
        return new PageQuery(0, 0, null);
    }

    /** The first {@code size} rows in the query's default order. */
    public static PageQuery first(int size) {
        return new PageQuery(1, size, null);
    }

    public boolean isUnpaged() {
        return size <= 0;
    }
}
