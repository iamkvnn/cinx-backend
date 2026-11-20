package com.cinx.learning.shared.infrastructure.persistence;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * Brings the stored children of an aggregate in line with the domain's list: a child the domain no longer lists is removed
 * (the mapping deletes orphans), and one it lists that is not stored yet is added. A child that is stored and still listed
 * is left exactly as it is: its own repository writes its changes, so a stale copy held by the parent never overwrites them.
 */
public final class ChildSync {
    private ChildSync() {
    }

    public static <E, D> void sync(Collection<E> stored, Collection<D> wanted, Function<E, String> storedId,
                                   Function<D, String> wantedId, Function<D, E> create) {
        Set<String> wantedIds = new HashSet<>();
        wanted.forEach(child -> {
            if (wantedId.apply(child) != null) {
                wantedIds.add(wantedId.apply(child));
            }
        });
        stored.removeIf(row -> !wantedIds.contains(storedId.apply(row)));
        Set<String> storedIds = new HashSet<>();
        stored.forEach(row -> storedIds.add(storedId.apply(row)));
        wanted.stream()
                .filter(child -> wantedId.apply(child) == null || !storedIds.contains(wantedId.apply(child)))
                .forEach(child -> stored.add(create.apply(child)));
    }
}
