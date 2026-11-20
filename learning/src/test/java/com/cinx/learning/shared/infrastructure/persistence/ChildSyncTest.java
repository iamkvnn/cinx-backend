package com.cinx.learning.shared.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChildSyncTest {
    private record Row(String id, String text) {
    }

    private record Child(String id, String text) {
    }

    private static void sync(List<Row> stored, List<Child> wanted) {
        ChildSync.sync(stored, wanted, Row::id, Child::id, child -> new Row(child.id(), child.text()));
    }

    @Test
    void aStoredChildTheDomainNoLongerListsIsRemoved() {
        List<Row> stored = new ArrayList<>(List.of(new Row("1", "one"), new Row("2", "two")));

        sync(stored, List.of(new Child("1", "one")));

        assertThat(stored).extracting(Row::id).containsExactly("1");
    }

    @Test
    void aNewChildWithoutAnIdIsAdded() {
        List<Row> stored = new ArrayList<>(List.of(new Row("1", "one")));

        sync(stored, List.of(new Child("1", "one"), new Child(null, "fresh")));

        assertThat(stored).extracting(Row::text).containsExactly("one", "fresh");
    }

    @Test
    void aListedChildThatIsAlreadyStoredIsLeftAsItIs() {
        Row kept = new Row("1", "stored text");
        List<Row> stored = new ArrayList<>(List.of(kept));

        // the parent holds a stale copy: its own repository owns the child's changes
        sync(stored, List.of(new Child("1", "stale text")));

        assertThat(stored).containsExactly(kept);
    }

    @Test
    void aChildListedWithAnIdThatIsNotStoredYetIsAdded() {
        List<Row> stored = new ArrayList<>();

        sync(stored, List.of(new Child("9", "assigned id")));

        assertThat(stored).extracting(Row::id).containsExactly("9");
    }

    @Test
    void anEmptyListRemovesEveryStoredChild() {
        List<Row> stored = new ArrayList<>(List.of(new Row("1", "one"), new Row("2", "two")));

        sync(stored, List.of());

        assertThat(stored).isEmpty();
    }
}
