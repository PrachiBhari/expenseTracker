package com.fintrack.category.dto;

import com.fintrack.category.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request body for POST /categories and PUT /categories/{id}.
 *
 * Same DTO is used for create AND update (PUT replaces the entire resource).
 * This keeps the API simple — the client always sends the full category shape.
 *
 * Validation rules:
 *   name  — required, max 80 chars (matches DB column length)
 *   type  — required, must be INCOME or EXPENSE (enforced by enum deserialization)
 *   color — optional, stores a hex color code like "#F8C8DC"
 *   icon  — optional, stores an icon identifier used by the frontend
 */
@Getter
@NoArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 80, message = "Category name must not exceed 80 characters")
    private String name;

    /**
     * Jackson deserializes "INCOME" or "EXPENSE" string → CategoryType enum.
     * If the string doesn't match, Jackson throws HttpMessageNotReadableException → 400.
     */
    @NotNull(message = "Category type is required (INCOME or EXPENSE)")
    private CategoryType type;

    @Size(max = 20, message = "Color must not exceed 20 characters")
    private String color;

    @Size(max = 40, message = "Icon must not exceed 40 characters")
    private String icon;
}
