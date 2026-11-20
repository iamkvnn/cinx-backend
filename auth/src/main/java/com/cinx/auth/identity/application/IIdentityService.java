package com.cinx.auth.identity.application;

import com.cinx.auth.identity.application.command.BanUserCommand;
import com.cinx.auth.identity.application.command.UpdateStaffRolesCommand;
import com.cinx.auth.identity.application.query.AuthorizationState;
import com.cinx.auth.identity.application.query.IdentityAccountView;

import java.util.List;

public interface IIdentityService {
    IdentityAccountView findAccount(String userId);
    IdentityAccountView ban(String userId, BanUserCommand command);
    IdentityAccountView unban(String userId);
    IdentityAccountView promoteInstructor(String userId);
    IdentityAccountView terminateInstructor(String userId);
    AuthorizationState authorizationState(String userId);
    List<String> findUserIdsByPermission(String permission);
    IdentityAccountView updateStaffRoles(String actorId, String userId, UpdateStaffRolesCommand command);
}
