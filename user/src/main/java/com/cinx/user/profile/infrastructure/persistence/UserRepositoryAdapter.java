package com.cinx.user.profile.infrastructure.persistence;

import com.cinx.user.profile.domain.User;
import com.cinx.user.profile.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class UserRepositoryAdapter implements UserRepository {
    private final UserJpaRepository jpa;
    private final UserPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUserId(String userId) {
        return jpa.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllByEmailIgnoreCase(String email) {
        return jpa.findAllByEmailIgnoreCase(email).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllByUserIdIn(List<String> userIds) {
        return jpa.findAllByUserIdIn(userIds).stream().map(mapper::toDomain).toList();
    }

    /** Inserts a new profile, or writes the changes onto the stored row so its key, lock version and audit columns stay put. */
    @Override
    @Transactional
    public User save(User user) {
        if (user.getId() == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(user)));
        }
        UserJpaEntity entity = jpa.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("Profile " + user.getId() + " no longer exists"));
        mapper.update(entity, user);
        return mapper.toDomain(jpa.save(entity));
    }
}
