package com.cinx.commerce.cart.application;

import com.cinx.commerce.cart.application.query.CartItemView;
import com.cinx.commerce.cart.domain.CartItem;
import com.cinx.commerce.cart.domain.CartRepository;
import com.cinx.commerce.catalog.application.SellableCourses;
import com.cinx.commerce.catalog.application.query.SellableCourseView;
import com.cinx.commerce.order.application.PurchaseHistory;
import com.cinx.common.exception.AlreadyExistException;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ConflictException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService implements ICartService, CartContents {
    private final CartRepository repository;
    private final SellableCourses courses;
    private final PurchaseHistory purchases;
    private final Clock clock;

    @Override
    @Transactional
    public CartItemView add(String userId, String courseId) {
        SellableCourseView course = courses.findSellable(courseId)
                .orElseThrow(() -> new BadRequestException(ErrorCode.COURSE_UNAVAILABLE_FOR_PURCHASE, "Course is not sellable"));
        // owned as far as commerce knows: bought in one of the user's paid orders
        if (purchases.paidCourseIds(userId).contains(courseId)) {
            throw new ConflictException(ErrorCode.COURSE_ALREADY_OWNED, "You already own this course");
        }
        if (repository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new AlreadyExistException("Course already exists in cart");
        }
        CartItem item = repository.save(CartItem.builder().id(UUID.randomUUID().toString())
                .userId(userId).courseId(course.courseId()).addedAt(LocalDateTime.now(clock)).build());
        return view(item, Optional.of(course), false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItemView> findAll(String userId) {
        Set<String> owned = purchases.paidCourseIds(userId);
        // a missing projection marks that one item unsellable instead of failing the whole cart
        return repository.findAllByUserId(userId).stream()
                .map(item -> view(item, courses.findById(item.getCourseId()), owned.contains(item.getCourseId())))
                .toList();
    }

    @Override
    @Transactional
    public void remove(String userId, String courseId) {
        if (!repository.deleteByUserIdAndCourseId(userId, courseId)) {
            throw new NotFoundException("Cart item not found");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> courseIdsOf(String userId) {
        return repository.findAllByUserId(userId).stream().map(CartItem::getCourseId).toList();
    }

    @Override
    @Transactional
    public void clear(String userId) {
        repository.deleteAllByUserId(userId);
    }

    private CartItemView view(CartItem item, Optional<SellableCourseView> course, boolean owned) {
        return new CartItemView(item.getId(), item.getCourseId(),
                course.map(SellableCourseView::title).orElse(null), course.map(SellableCourseView::price).orElse(null),
                item.getAddedAt(), course.map(SellableCourseView::sellable).orElse(false), owned);
    }
}
