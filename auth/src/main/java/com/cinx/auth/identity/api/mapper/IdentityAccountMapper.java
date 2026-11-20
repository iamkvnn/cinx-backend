package com.cinx.auth.identity.api.mapper;

import com.cinx.auth.identity.api.dto.AuthorizationStateResponse;
import com.cinx.auth.identity.api.dto.IdentityAccountResponse;
import com.cinx.auth.identity.api.dto.UpdateRolesRequest;
import com.cinx.auth.identity.api.dto.request.BanUserRequest;
import com.cinx.auth.identity.application.command.BanUserCommand;
import com.cinx.auth.identity.application.command.UpdateStaffRolesCommand;
import com.cinx.auth.identity.application.query.AuthorizationState;
import com.cinx.auth.identity.application.query.IdentityAccountView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IdentityAccountMapper {
    IdentityAccountResponse toDto(IdentityAccountView view);

    AuthorizationStateResponse toDto(AuthorizationState state);

    BanUserCommand toCommand(BanUserRequest request);

    /** The request's {@code reason} is validated at the edge; the service does not use it. */
    UpdateStaffRolesCommand toCommand(UpdateRolesRequest request);
}
