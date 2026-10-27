package com.cinx.learning.engagement.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserExperience {
    private String userId;
    private Integer xp;
    private LocalDateTime updatedAt;
    private Long version;
}
