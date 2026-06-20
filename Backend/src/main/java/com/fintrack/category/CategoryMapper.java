package com.fintrack.category;

import com.fintrack.category.dto.CategoryRequest;
import com.fintrack.category.dto.CategoryResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for Category ↔ DTO conversions.
 *
 * WHY MapStruct?
 * It generates plain Java mapping code at compile time — no reflection, no proxies.
 * The generated code is as fast as hand-written mapping.
 * Errors are caught at build time (wrong field name = compile error, not runtime NPE).
 *
 * componentModel = "spring":
 *   MapStruct generates the implementation as a @Component.
 *   Spring auto-discovers and injects it as a @Bean wherever @Autowired or constructor injection is used.
 *   (Also set globally via -Amapstruct.defaultComponentModel=spring in pom.xml — explicit here for clarity.)
 *
 * toResponse(Category):
 *   Category → CategoryResponse.
 *   MapStruct matches fields by name: category.name → response.name, category.type → response.type etc.
 *   It also maps inherited fields from BaseEntity: createdAt.
 *
 * toEntity(CategoryRequest):
 *   CategoryRequest → new Category entity.
 *   We IGNORE id, user, createdAt, updatedAt — these are set by JPA/service, not by the client.
 *
 * updateEntity(CategoryRequest, @MappingTarget Category):
 *   @MappingTarget: MapStruct updates the EXISTING entity in-place instead of creating a new one.
 *   This is used for PUT (update) operations.
 *   Same ignores as toEntity — we never allow the client to change id, user, or audit fields.
 */
@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Category toEntity(CategoryRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CategoryRequest request, @MappingTarget Category category);
}
