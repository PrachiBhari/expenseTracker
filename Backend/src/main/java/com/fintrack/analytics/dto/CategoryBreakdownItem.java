package com.fintrack.analytics.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * One slice of the category-breakdown pie/donut chart.
 *
 * Returned by GET /analytics/by-category?type=EXPENSE&from=&to=
 *
 * Each item represents one category and how much was spent (or earned) in it.
 * The list is ordered by total DESC so the biggest categories come first —
 * useful for immediately highlighting the top spending areas.
 *
 * color and icon are included so the frontend can render the chart
 * without a second request for category metadata.
 */
@Getter
@Builder
public class CategoryBreakdownItem {

    private Long categoryId;
    private String name;
    private String color;
    private String icon;
    private BigDecimal total;
}
