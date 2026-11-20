package com.cinx.learning.engagement.application;

import com.cinx.learning.engagement.domain.UserExperience;
import com.cinx.learning.engagement.domain.UserExperienceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserExperienceService {
    private final UserExperienceRepository repository;

    @Transactional
    public UserExperience award(String userId, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("XP amount must be positive");
        }
        UserExperience experience = repository.findById(userId)
                .orElseGet(() -> UserExperience.builder().userId(userId).xp(0).build());
        experience.setXp(experience.getXp() + amount);
        return repository.save(experience);
    }
}
