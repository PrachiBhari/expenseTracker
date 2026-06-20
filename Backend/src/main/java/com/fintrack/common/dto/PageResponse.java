package com.fintrack.common.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic paginated response envelope.
 *
 * Wraps Spring's Page<T> into a clean, consistent JSON shape.
 * Spring's default Page<T> serialization includes many internal fields
 * that the frontend doesn't need and makes the contract harder to work with.
 *
 * This DTO gives the frontend exactly what it needs:
 *   content       — the actual list of items for this page
 *   page          — current page number (0-based)
 *   size          — number of items per page (requested)
 *   totalElements — total items across all pages
 *   totalPages    — total number of pages
 *   last          — true if this is the last page (for "load more" logic)
 *
 * Generic: PageResponse<ExpenseResponse>, PageResponse<IncomeResponse>, etc.
 *
 * Usage in service:
 *   Page<Expense> page = expenseRepository.findAll(spec, pageable);
 *   Page<ExpenseResponse> dtoPage = page.map(expenseMapper::toResponse);
 *   return PageResponse.from(dtoPage);
 */
@Getter
@Builder
public class PageResponse<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean last;

    /**
     * Static factory method — converts Spring's Page<T> into our PageResponse<T>.
     * Generic method so it works with any DTO type T.
     */
    public static <T> PageResponse<T> from(Page<T> pageResult) {
        return PageResponse.<T>builder()
                .content(pageResult.getContent())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }
}
