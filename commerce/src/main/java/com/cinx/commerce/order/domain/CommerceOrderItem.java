package com.cinx.commerce.order.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** One course on an order, with the title and price it was sold at. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommerceOrderItem {
    private String id;
    private String courseId;
    private String titleSnapshot;
    private BigDecimal priceSnapshot;
    /** The course owner when the line was sold (from the sellable-course projection). */
    private String instructorId;
    /** What this line brought in after the order's voucher, split across the lines it applied to. */
    private BigDecimal allocatedAmount;
    /** The platform fee percent in force when the line was sold. */
    private BigDecimal platformFeePercent;
}
