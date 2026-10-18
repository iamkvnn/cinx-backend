package com.cinx.user.policy.domain;

import java.util.List;

/** Port: where users' agreements to policies are stored. */
public interface PolicyAcceptanceRepository {
    List<PolicyAcceptance> saveAll(List<PolicyAcceptance> acceptances);
}
