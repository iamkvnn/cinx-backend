package com.cinx.learning.enrollment.application;

import com.cinx.learning.enrollment.application.mapper.EnrollmentMapper;
import com.cinx.learning.enrollment.application.query.EnrollmentView;
import com.cinx.learning.enrollment.domain.CourseEntitlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService implements IEnrollmentService {
    private final CourseEntitlementRepository entitlements;
    private final EnrollmentMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentView> findMine(String userId) {
        return entitlements.findAllByUserIdOrderByGrantedAtDesc(userId).stream().map(mapper::toResponse).toList();
    }
}
