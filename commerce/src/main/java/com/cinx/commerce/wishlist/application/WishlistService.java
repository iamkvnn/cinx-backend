package com.cinx.commerce.wishlist.application;

import com.cinx.commerce.catalog.application.SellableCourses;
import com.cinx.commerce.catalog.application.query.SellableCourseView;
import com.cinx.commerce.shared.application.DomainEvents;
import com.cinx.commerce.wishlist.application.query.WishlistEntryView;
import com.cinx.commerce.wishlist.domain.WishlistItem;
import com.cinx.commerce.wishlist.domain.WishlistRepository;
import com.cinx.common.exception.AlreadyExistException;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WishlistService implements IWishlistService {
    private final WishlistRepository repository;
    private final SellableCourses courses;
    private final DomainEvents events;

    @Override
    @Transactional
    public WishlistEntryView add(String userId, String courseId) {
        SellableCourseView course = courses.findSellable(courseId)
                .orElseThrow(() -> new BadRequestException("Course is not sellable"));
        if (repository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new AlreadyExistException("Course already exists in wishlist");
        }
        WishlistItem item = repository.save(WishlistItem.builder().id(UUID.randomUUID().toString())
                .userId(userId).courseId(courseId).addedAt(LocalDateTime.now()).build());
        events.publish("CommerceWishlistAdded", item.getId(), Map.of("userId", userId, "courseId", courseId, "added", true));
        return entry(item, course);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistEntryView> findAll(String userId) {
        return repository.findAllByUserId(userId).stream()
                .map(item -> entry(item, courses.findById(item.getCourseId())
                        .orElseThrow(() -> new NotFoundException("Course projection not found")))).toList();
    }

    @Override
    @Transactional
    public void remove(String userId, String courseId) {
        if (!repository.deleteByUserIdAndCourseId(userId, courseId)) {
            throw new NotFoundException("Wishlist item not found");
        }
        events.publish("CommerceWishlistRemoved", userId + ":" + courseId,
                Map.of("userId", userId, "courseId", courseId, "added", false));
    }

    private WishlistEntryView entry(WishlistItem item, SellableCourseView course) {
        return new WishlistEntryView(item.getCourseId(), course.title(), course.price(), item.getAddedAt());
    }
}
