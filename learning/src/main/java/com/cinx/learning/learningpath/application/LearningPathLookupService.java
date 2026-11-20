package com.cinx.learning.learningpath.application;

import com.cinx.learning.learningpath.application.query.UserLearningPathInfo;
import com.cinx.learning.learningpath.domain.UserLearningPathRepository;
import com.cinx.learning.shared.domain.LearningPathStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LearningPathLookupService implements LearningPathLookup {
    private final UserLearningPathRepository pathRepository;

    @Override
    public Optional<UserLearningPathInfo> currentPath(String userId) {
        return pathRepository.findByUserIdAndStatusIn(userId, List.of(LearningPathStatus.ACTIVE, LearningPathStatus.PENDING_PAYMENT))
                .map(UserLearningPathInfo::of);
    }

    @Override
    public Optional<UserLearningPathInfo> path(String pathId) {
        return pathRepository.findById(pathId).map(UserLearningPathInfo::of);
    }
}
