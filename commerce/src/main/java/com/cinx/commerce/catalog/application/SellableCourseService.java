package com.cinx.commerce.catalog.application;

import com.cinx.commerce.catalog.application.query.SellableCourseView;
import com.cinx.commerce.catalog.domain.SellableCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SellableCourseService implements SellableCourses {
    private final SellableCourseRepository courses;

    @Override
    @Transactional(readOnly = true)
    public Optional<SellableCourseView> findById(String courseId) {
        return courses.findById(courseId).map(SellableCourseView::of);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SellableCourseView> findSellable(String courseId) {
        return findById(courseId).filter(SellableCourseView::sellable);
    }
}
