package com.cinx.auth.identity.application.query;

import java.time.LocalDateTime;
import java.util.List;

/** Port for reads across many accounts, so none of them is loaded as an aggregate. */
public interface AccountAuthorizationQueries {
    /** Every active account with the roles it holds at {@code now}; an account with none has an empty set. */
    List<AccountRoles> findActiveAccountRoles(LocalDateTime now);
}
