package com.cinx.user.profile.application.command;

import com.cinx.user.profile.domain.PartnershipTerminationReasonType;

public record TerminatePartnershipCommand(PartnershipTerminationReasonType reasonType, String reasonDetail) {
}
