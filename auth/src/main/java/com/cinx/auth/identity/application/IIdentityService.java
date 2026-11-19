package com.cinx.auth.identity.application;

import com.cinx.auth.identity.application.command.BanUserCommand;
import com.cinx.auth.identity.application.command.UpdateStaffRolesCommand;
import com.cinx.auth.identity.application.query.IdentityAccountView;

import java.util.List;

public interface IIdentityService {
    IdentityAccountView findAccount(String userId);
    IdentityAccountView ban(String userId, BanUserCommand command);
    IdentityAccountView unban(String userId);
    /** Users whose timed ban has ended, earliest first, at most {@code limit}. */
    List<String> findExpiredBans(int limit);
    /** Reactivates the user if their ban is still in force and has ended; otherwise does nothing. */
    void liftExpiredBan(String userId);
    IdentityAccountView promoteInstructor(String userId);
    IdentityAccountView terminateInstructor(String userId);
    List<String> findUserIdsByPermission(String permission);
    IdentityAccountView updateStaffRoles(String actorId, String userId, UpdateStaffRolesCommand command);
}
