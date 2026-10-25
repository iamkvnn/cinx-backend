package com.cinx.user.profile.api.mapper;

import com.cinx.user.instructor.api.dto.InstructorLookupDto;
import com.cinx.user.instructor.api.dto.request.InstructorApplicationRequest;
import com.cinx.user.instructor.api.dto.request.TerminatePartnershipRequest;
import com.cinx.user.profile.api.dto.CreateUserRequest;
import com.cinx.user.profile.api.dto.UpdateProfileRequest;
import com.cinx.user.profile.api.dto.UserDto;
import com.cinx.user.profile.api.dto.LearnerProfileDto;
import com.cinx.user.profile.api.dto.request.LearnerProfileAnswersRequest;
import com.cinx.user.profile.api.dto.request.LearnerProfileRequest;
import com.cinx.user.profile.api.dto.request.OnboardProfileRequest;
import com.cinx.user.profile.application.command.CreateUserCommand;
import com.cinx.user.profile.application.command.InstructorApplicationCommand;
import com.cinx.user.profile.application.command.LearnerProfileAnswers;
import com.cinx.user.profile.application.command.OnboardProfileCommand;
import com.cinx.user.profile.application.command.SaveLearnerProfileCommand;
import com.cinx.user.profile.application.query.LearnerProfileView;
import com.cinx.user.profile.application.command.TerminatePartnershipCommand;
import com.cinx.user.profile.application.command.UpdateProfileCommand;
import com.cinx.user.profile.application.query.InstructorLookup;
import com.cinx.user.profile.application.query.UserView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(UserView view);

    InstructorLookupDto toDto(InstructorLookup lookup);

    CreateUserCommand toCommand(CreateUserRequest request);

    UpdateProfileCommand toCommand(UpdateProfileRequest request);

    OnboardProfileCommand toCommand(OnboardProfileRequest request);

    LearnerProfileAnswers toCommand(LearnerProfileAnswersRequest request);

    SaveLearnerProfileCommand toCommand(LearnerProfileRequest request);

    LearnerProfileDto toDto(LearnerProfileView view);

    InstructorApplicationCommand toCommand(InstructorApplicationRequest request);

    TerminatePartnershipCommand toCommand(TerminatePartnershipRequest request);
}
