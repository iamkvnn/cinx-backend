package com.cinx.commerce.catalog.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellableCourse {
    private String courseId;
    private String title;
    private BigDecimal price;
    private String categoryId;
    private String instructorId;
    private boolean sellable;
    private LocalDateTime updatedAt;
}
