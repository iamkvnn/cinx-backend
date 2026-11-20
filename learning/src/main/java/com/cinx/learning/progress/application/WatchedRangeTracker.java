package com.cinx.learning.progress.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchedRangeTracker {
    private final ObjectMapper objectMapper;

    public String merge(String existingJson, int start, int end, Integer duration) {
        int clampedStart = Math.max(0, clampPosition(start, duration));
        int clampedEnd = Math.max(0, clampPosition(end, duration));
        if (clampedEnd <= clampedStart) {
            return existingJson;
        }

        List<List<Integer>> ranges = parse(existingJson);
        ranges.add(List.of(clampedStart, clampedEnd));
        ranges.sort(Comparator.comparingInt(range -> range.get(0)));

        List<List<Integer>> merged = new ArrayList<>();
        for (List<Integer> range : ranges) {
            if (range.size() < 2 || range.get(1) <= range.get(0)) {
                continue;
            }
            if (merged.isEmpty() || range.get(0) > merged.getLast().get(1)) {
                merged.add(new ArrayList<>(List.of(range.get(0), range.get(1))));
            } else {
                List<Integer> last = merged.getLast();
                last.set(1, Math.max(last.get(1), range.get(1)));
            }
        }

        try {
            return objectMapper.writeValueAsString(merged);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize watched ranges", ex);
            return existingJson;
        }
    }

    public int watchedSeconds(String watchedRangesJson) {
        return parse(watchedRangesJson).stream()
                .filter(range -> range.size() >= 2 && range.get(1) > range.get(0))
                .mapToInt(range -> range.get(1) - range.get(0))
                .sum();
    }

    private List<List<Integer>> parse(String watchedRangesJson) {
        if (watchedRangesJson == null || watchedRangesJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(objectMapper.readValue(watchedRangesJson, new TypeReference<List<List<Integer>>>() {}));
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse watched ranges", ex);
            return new ArrayList<>();
        }
    }

    private int clampPosition(Integer position, Integer duration) {
        if (duration != null && duration > 0) {
            return Math.min(position, duration);
        }
        return position;
    }
}
