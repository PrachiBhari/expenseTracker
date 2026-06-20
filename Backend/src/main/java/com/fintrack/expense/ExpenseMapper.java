package com.fintrack.expense;

import com.fintrack.category.Category;
import com.fintrack.expense.dto.ExpenseRequest;
import com.fintrack.expense.dto.ExpenseResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for Expense ↔ DTO conversions.
 *
 * NESTED OBJECT MAPPING:
 * Expense has a 'category' field of type Category (entity).
 * ExpenseResponse has a 'category' field of type CategorySummary (inner DTO).
 * MapStruct handles this with a second mapping method: toCategorySummary(Category).
 * It automatically discovers and uses it when mapping Expense → ExpenseResponse.
 *
 * WHY IGNORE category IN toEntity / updateEntity?
 * ExpenseRequest.categoryId is a Long (just the FK id).
 * Expense.category is a Category entity (full object).
 * MapStruct cannot automatically resolve Long → Category (that requires a DB lookup).
 * We ignore it here and set it manually in the service after loading the Category from DB.
 *
 * GENERATED CODE (what MapStruct creates for toResponse):
 *   ExpenseResponse.CategorySummary summary = new ExpenseResponse.CategorySummary.ExpenseResponseCategorySummaryBuilder()
 *       .id(expense.getCategory().getId())
 *       .name(expense.getCategory().getName())
 *       .color(expense.getCategory().getColor())
 *       .icon(expense.getCategory().getIcon())
 *       .build();
 *   return ExpenseResponse.builder()
 *       .id(expense.getId())
 *       .amount(expense.getAmount())
 *       .category(summary)
 *       ... etc
 *       .build();
 * All this generated automatically — we write 0 lines of this.
 */
@Mapper(componentModel = "spring")
public interface ExpenseMapper {

    /**
     * Maps Expense entity + nested Category → ExpenseResponse with CategorySummary.
     * The 'category' field mapping is handled by the toCategorySummary method below.
     */
    @Mapping(target = "category", source = "category")
    ExpenseResponse toResponse(Expense expense);

    /**
     * Nested mapping: Category entity → ExpenseResponse.CategorySummary.
     * MapStruct uses this automatically when mapping the 'category' field in toResponse().
     */
    ExpenseResponse.CategorySummary toCategorySummary(Category category);

    /**
     * Maps ExpenseRequest → new Expense entity.
     * Ignores: id (not from client), user (set in service), category (set in service),
     *          createdAt/updatedAt (managed by JPA auditing).
     * Note: 'categoryId' in request has no matching field in Expense (Expense has 'category').
     *       MapStruct ignores unmatched source fields by default.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Expense toEntity(ExpenseRequest request);

    /**
     * Updates an existing Expense entity in-place from an ExpenseRequest.
     * @MappingTarget = update the existing object, don't create a new one.
     * Only updates: amount, description, expenseDate, paymentMethod.
     * Category is updated separately in the service (requires a DB lookup).
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(ExpenseRequest request, @MappingTarget Expense expense);
}
