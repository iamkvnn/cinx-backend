package com.cinx.learning.shared.domain;

public enum CourseMembershipRole {
    OWNER,
    MANAGER,
    EDITOR;

    public boolean canEdit() {
        return this == OWNER || this == MANAGER || this == EDITOR;
    }

    public boolean canSubmit() {
        return this == OWNER || this == MANAGER;
    }

    public boolean canTeach() {
        return this == OWNER || this == MANAGER;
    }

    public boolean canManageTeam() {
        return this == OWNER;
    }

    public boolean canArchive() {
        return this == OWNER;
    }
}
