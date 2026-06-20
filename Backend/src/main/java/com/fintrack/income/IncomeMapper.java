package com.fintrack.income;

import com.fintrack.category.Category;
import com.fintrack.income.dto.IncomeRequest;
import com.fintrack.income.dto.IncomeResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for Income ↔ DTO conversions.
 *
 * Identical pattern to ExpenseMapper with one nuance:
 * Income.category is NULLABLE — MapStruct handles this gracefully.
 * When income.getCategory() is null, toCategorySummary(null) is called.
 * MapStruct generates: if (source == null) return null;
 * So IncomeResponse.category will be null for uncategorized income. ✓
 */
@Mapper(componentModel = "spring")
public interface IncomeMapper {

    @Mapping(target = "category", source = "category")
    IncomeResponse toResponse(Income income);

    IncomeResponse.CategorySummary toCategorySummary(Category category);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Income toEntity(IncomeRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(IncomeRequest request, @MappingTarget Income income);
}
