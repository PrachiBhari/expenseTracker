package com.fintrack.category;

import com.fintrack.category.dto.CategoryRequest;
import com.fintrack.category.dto.CategoryResponse;
import com.fintrack.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Category CRUD.
 * All endpoints are PROTECTED — require a valid Bearer JWT.
 *
 * Full paths (context-path = /api/v1):
 *   GET    /api/v1/categories           → list all categories (optionally filter by type)
 *   POST   /api/v1/categories           → create a category
 *   GET    /api/v1/categories/{id}      → get one category
 *   PUT    /api/v1/categories/{id}      → replace a category
 *   DELETE /api/v1/categories/{id}      → delete a category (blocked if in use)
 *
 * Controller does ONLY these things:
 *   1. Extract the current userId from SecurityContext
 *   2. Pass userId + validated request to the service
 *   3. Return the correct HTTP status code + body
 *
 * @SecurityRequirement("bearerAuth") links to the Swagger Bearer scheme from OpenApiConfig.
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Manage income and expense categories")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "List all categories for the current user",
               description = "Optionally filter by type: ?type=EXPENSE or ?type=INCOME")
    public ResponseEntity<List<CategoryResponse>> getAllCategories(
            @RequestParam(required = false) CategoryType type) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(categoryService.getAllCategories(userId, type));
    }

    @PostMapping
    @Operation(summary = "Create a new category")
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        CategoryResponse response = categoryService.createCategory(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single category by ID")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(categoryService.getCategoryById(id, userId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a category")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(categoryService.updateCategory(id, request, userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category (blocked if it has transactions)")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        categoryService.deleteCategory(id, userId);
        return ResponseEntity.noContent().build();
    }
}
