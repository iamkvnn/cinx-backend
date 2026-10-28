package com.cinx.learning.catalog.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class OrderIndexUtils {
    public static final int ORDER_STEP = 1024;

    private OrderIndexUtils() {
    }

    public static <T> int insertionIndex(
            List<T> orderedItems,
            String previousId,
            String nextId,
            Function<T, String> idExtractor,
            String movedId,
            String itemName
    ) {
        if (previousId != null && Objects.equals(previousId, movedId)) {
            throw new BadRequestException(ErrorCode.LESSON_ORDER_INVALID, "Previous " + itemName + " cannot be the moved " + itemName);
        }
        if (nextId != null && Objects.equals(nextId, movedId)) {
            throw new BadRequestException(ErrorCode.LESSON_ORDER_INVALID, "Next " + itemName + " cannot be the moved " + itemName);
        }
        if (previousId == null && nextId == null) {
            if (orderedItems.isEmpty()) {
                return 0;
            }
            throw new BadRequestException(ErrorCode.LESSON_ORDER_INVALID, "Move position must include previous or next " + itemName);
        }

        int previousIndex = previousId == null ? -1 : indexOf(orderedItems, previousId, idExtractor);
        int nextIndex = nextId == null ? -1 : indexOf(orderedItems, nextId, idExtractor);
        if (previousId != null && previousIndex < 0) {
            throw new BadRequestException(ErrorCode.LESSON_ORDER_INVALID, "Previous " + itemName + " does not belong to the target position: " + previousId);
        }
        if (nextId != null && nextIndex < 0) {
            throw new BadRequestException(ErrorCode.LESSON_ORDER_INVALID, "Next " + itemName + " does not belong to the target position: " + nextId);
        }
        if (previousId != null && nextId != null) {
            if (previousIndex + 1 != nextIndex) {
                throw new BadRequestException(ErrorCode.LESSON_ORDER_INVALID, "Previous and next " + itemName + " must be adjacent");
            }
            return nextIndex;
        }
        return previousId != null ? previousIndex + 1 : nextIndex;
    }

    public static <T> Integer midpointOrderIndex(
            List<T> orderedItems,
            int insertionIndex,
            Function<T, Integer> orderIndexExtractor
    ) {
        Integer previous = insertionIndex == 0 ? null : orderIndexExtractor.apply(orderedItems.get(insertionIndex - 1));
        Integer next = insertionIndex == orderedItems.size() ? null : orderIndexExtractor.apply(orderedItems.get(insertionIndex));
        return midpoint(previous, next);
    }

    public static <T> List<T> rebalance(
            List<T> orderedItems,
            Function<T, Integer> orderIndexExtractor,
            BiConsumer<T, Integer> orderIndexSetter
    ) {
        List<T> changedItems = new ArrayList<>();
        for (int i = 0; i < orderedItems.size(); i++) {
            T item = orderedItems.get(i);
            int orderIndex = (i + 1) * ORDER_STEP;
            if (!Objects.equals(orderIndexExtractor.apply(item), orderIndex)) {
                orderIndexSetter.accept(item, orderIndex);
                changedItems.add(item);
            }
        }
        return changedItems;
    }

    private static Integer midpoint(Integer previous, Integer next) {
        if (previous == null && next == null) {
            return ORDER_STEP;
        }
        if (previous == null) {
            return next > 1 ? next / 2 : null;
        }
        if (next == null) {
            return previous + ORDER_STEP;
        }
        return next - previous > 1 ? previous + (next - previous) / 2 : null;
    }

    private static <T> int indexOf(List<T> orderedItems, String id, Function<T, String> idExtractor) {
        for (int i = 0; i < orderedItems.size(); i++) {
            if (Objects.equals(idExtractor.apply(orderedItems.get(i)), id)) {
                return i;
            }
        }
        return -1;
    }
}

