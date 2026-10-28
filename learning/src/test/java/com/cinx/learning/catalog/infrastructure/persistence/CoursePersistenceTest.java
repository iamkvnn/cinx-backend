package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.learning.catalog.domain.Course;
import com.cinx.learning.catalog.domain.CourseImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoursePersistenceTest {
    @Mock
    CourseJpaRepository jpa;

    CatalogEntityRefs refs = mock(CatalogEntityRefs.class);
    CourseImagePersistenceMapper imageMapper = new CourseImagePersistenceMapperImpl(refs);
    CoursePersistenceMapper mapper = new CoursePersistenceMapperImpl(imageMapper, refs);
    CourseRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CourseRepositoryAdapter(jpa, mapper, imageMapper);
    }

    @Test
    void aStoredCourseComesBackWithItsCategoryAsAnIdAndItsImagesAsOwnedChildren() {
        CourseJpaEntity stored = entity("course-1", "Title");
        stored.setCategory(category("cat-1"));
        stored.getImages().add(image("img-1", "a.png", 0));

        Course course = mapper.toDomain(stored);

        assertThat(course.getCategoryId()).isEqualTo("cat-1");
        assertThat(course.getImages()).extracting(CourseImage::getId, CourseImage::getImageUrl).containsExactly(org.assertj.core.groups.Tuple.tuple("img-1", "a.png"));
    }

    @Test
    void aChangeIsWrittenOntoTheStoredRowAndTheStoredVersionIsLeftToTheStore() {
        CourseJpaEntity stored = entity("course-1", "Old title");
        stored.setVersion(7L);
        when(jpa.findById("course-1")).thenReturn(Optional.of(stored));
        when(jpa.save(stored)).thenReturn(stored);
        Course changed = mapper.toDomain(stored);
        changed.setTitle("New title");
        changed.setVersion(3L);

        adapter.save(changed);

        assertThat(stored.getTitle()).isEqualTo("New title");
        assertThat(stored.getVersion()).isEqualTo(7L);
        verify(jpa).save(stored);
    }

    @Test
    void imagesTheCourseNoLongerListsAreDroppedAndNewOnesAreAddedBackPointingAtTheCourse() {
        CourseJpaEntity stored = entity("course-1", "Title");
        stored.getImages().add(image("keep", "keep.png", 0));
        stored.getImages().add(image("drop", "drop.png", 1));
        when(jpa.findById("course-1")).thenReturn(Optional.of(stored));
        when(jpa.save(stored)).thenReturn(stored);
        Course changed = mapper.toDomain(stored);
        changed.getImages().removeIf(image -> "drop".equals(image.getId()));
        changed.getImages().add(CourseImage.builder().imageUrl("new.png").publicId("new").sortOrder(2).build());

        adapter.save(changed);

        assertThat(stored.getImages()).extracting(CourseImageJpaEntity::getImageUrl).containsExactly("keep.png", "new.png");
        assertThat(stored.getImages()).allSatisfy(row -> assertThat(row.getCourse()).isSameAs(stored));
    }

    @Test
    void aKeptImageIsNotOverwrittenByTheStaleCopyTheCourseHolds() {
        CourseJpaEntity stored = entity("course-1", "Title");
        CourseImageJpaEntity kept = image("keep", "keep.png", 0);
        stored.getImages().add(kept);
        when(jpa.findById("course-1")).thenReturn(Optional.of(stored));
        when(jpa.save(stored)).thenReturn(stored);
        Course changed = mapper.toDomain(stored);
        changed.getImages().get(0).setImageUrl("stale.png");

        adapter.save(changed);

        assertThat(stored.getImages()).containsExactly(kept);
        assertThat(kept.getImageUrl()).isEqualTo("keep.png");
    }

    @Test
    void aNewCourseIsInserted() {
        when(jpa.save(any(CourseJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Course saved = adapter.save(Course.builder().title("Fresh").instructorId("owner-1").build());

        assertThat(saved.getTitle()).isEqualTo("Fresh");
        verify(jpa).save(any(CourseJpaEntity.class));
    }

    @Test
    void thePublicSearchSortsCoursesWithoutARatingLastAndPassesTheSortAlong() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(jpa.searchPublished(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), pageable.capture()))
                .thenReturn(new PageImpl<>(List.of(entity("course-1", "A"))));

        PageResult<Course> result = adapter.searchPublished(null, null, null, null, null, null, null, null, null, null,
                new PageQuery(1, 12, "{\"rating\":\"DESC\",\"title\":\"ASC\"}"));

        assertThat(pageable.getValue().getSort().getOrderFor("rating").getNullHandling()).isEqualTo(Sort.NullHandling.NULLS_LAST);
        assertThat(pageable.getValue().getSort().getOrderFor("title").getNullHandling()).isEqualTo(Sort.NullHandling.NATIVE);
        assertThat(result.content()).extracting(Course::getId).containsExactly("course-1");
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(12);
    }

    private CourseJpaEntity entity(String id, String title) {
        CourseJpaEntity entity = new CourseJpaEntity();
        entity.setId(id);
        entity.setTitle(title);
        entity.setImages(new ArrayList<>());
        entity.setOutcomes(new ArrayList<>());
        return entity;
    }

    private CategoryJpaEntity category(String id) {
        CategoryJpaEntity category = new CategoryJpaEntity();
        category.setId(id);
        return category;
    }

    private CourseImageJpaEntity image(String id, String url, int sortOrder) {
        CourseImageJpaEntity image = new CourseImageJpaEntity();
        image.setId(id);
        image.setImageUrl(url);
        image.setSortOrder(sortOrder);
        return image;
    }
}
