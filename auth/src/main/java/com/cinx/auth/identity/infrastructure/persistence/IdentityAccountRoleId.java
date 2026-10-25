package com.cinx.auth.identity.infrastructure.persistence;

import com.cinx.auth.identity.domain.AuthorizationRole;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class IdentityAccountRoleId implements Serializable {
    private String userId;
    private AuthorizationRole roleCode;
}
