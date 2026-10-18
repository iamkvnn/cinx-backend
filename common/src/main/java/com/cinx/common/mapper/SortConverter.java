package com.cinx.common.mapper;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.stream.Collectors;

public class SortConverter {

    /** The field names a sort asks for, in order, without building any persistence type; the format is checked like {@link #toSort}. */
    public static java.util.List<String> properties(String sort) {
        return toSort(sort).stream().map(Sort.Order::getProperty).toList();
    }

    /**
     * Expects a JSON string in the format: {"field1": "ASC", "field2": "DESC"}
     * Returns a Sort object that can be used in Spring Data queries.
     */
    public static Sort toSort(String sort) {
        Sort s = Sort.unsorted();
        if (sort != null && !sort.isBlank()) {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> sortMap;
            try {
                sortMap = mapper.readValue(sort, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                throw new BadRequestException(ErrorCode.INVALID_SORT, "Invalid sort parameter format");
            }
            s = sortMap.entrySet().stream()
                    .map(e -> new Sort.Order(Sort.Direction.fromString(e.getValue()), e.getKey()))
                    .collect(Collectors.collectingAndThen(Collectors.toList(), Sort::by));
        }

        return s;
    }
}
