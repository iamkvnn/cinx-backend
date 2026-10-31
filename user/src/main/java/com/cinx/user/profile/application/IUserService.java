package com.cinx.user.profile.application;

import com.cinx.common.paging.PageResult;
import com.cinx.user.profile.application.command.CreateUserCommand;
import com.cinx.user.profile.application.command.InstructorApplicationCommand;
import com.cinx.user.profile.application.command.OnboardProfileCommand;
import com.cinx.user.profile.application.command.TerminatePartnershipCommand;
import com.cinx.user.profile.application.command.UpdateProfileCommand;
import com.cinx.user.profile.application.query.InstructorLookup;
import com.cinx.user.profile.application.query.UserView;
import com.cinx.user.profile.domain.AccountType;
import com.cinx.user.profile.domain.InstructorApplicationStatus;

import java.util.List;

public interface IUserService {
    PageResult<UserView> findAll(int page, int size, String query, Boolean isInstructorVerified,
                                 InstructorApplicationStatus applicationStatus, String sort);
    UserView findByEmail(String email);
    UserView findByUserId(String userId);
    Boolean checkInstructorVerified(String id);
    InstructorLookup lookupVerifiedInstructor(String email);
    UserView createUser(CreateUserCommand command);
    void verifyInstructor(String id);
    void rejectInstructor(String id, String reason);
    void terminatePartnership(String id, TerminatePartnershipCommand command);
    UserView applyInstructor(String userId, InstructorApplicationCommand command);
    UserView updateProfile(String id, UpdateProfileCommand command);
    void updateLastAccess(String userId);
    List<UserView> findByIds(List<String> ids);
    void updatePreferredCategories(String userId, List<String> categoryIds);
    UserView updateEffects(String userId, boolean enabled);
    UserView onboardProfile(String userId, String keycloakSubject, String email, AccountType accountType, OnboardProfileCommand command);
    UserView onboardProfile(String userId, String keycloakSubject, String email, AccountType accountType,
                            OnboardProfileCommand command, String idempotencyKey);
    long countTotalUsers();
    long countUsersBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}
