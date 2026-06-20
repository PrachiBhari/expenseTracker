package com.fintrack.category;

import com.fintrack.category.dto.CategoryRequest;
import com.fintrack.category.dto.CategoryResponse;
import com.fintrack.common.exception.CategoryInUseException;
import com.fintrack.common.exception.DuplicateResourceException;
import com.fintrack.common.exception.ResourceNotFoundException;
import com.fintrack.expense.ExpenseRepository;
import com.fintrack.income.IncomeRepository;
import com.fintrack.user.User;
import com.fintrack.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CategoryService.
 *
 * Key behaviours tested:
 * 1. getAllCategories — type filter applied when provided, omitted when null
 * 2. getCategoryById — returns 404 when category not found or belongs to another user
 * 3. createCategory — saves on success, throws 409 on duplicate (name + type + userId)
 * 4. deleteCategory — blocks deletion when category has expense or income records
 *
 * INTERVIEW: "Why do we mock CategoryMapper instead of using the real implementation?"
 *   The real CategoryMapper is a MapStruct-generated class compiled at build time.
 *   In unit tests (no Spring context), MapStruct beans aren't instantiated.
 *   Mocking it keeps the test focused on CategoryService logic, not mapping logic.
 *   Mapper correctness is verified separately by looking at compile-time generation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Unit Tests")
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private IncomeRepository incomeRepository;
    @Mock private UserRepository userRepository;
    @Mock private CategoryMapper categoryMapper;

    @InjectMocks private CategoryService categoryService;

    private static final Long USER_ID = 1L;
    private static final Long CATEGORY_ID = 10L;

    private User testUser;
    private Category testCategory;
    private CategoryResponse testResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID)
                .email("user@example.com")
                .build();

        testCategory = Category.builder()
                .id(CATEGORY_ID)
                .user(testUser)
                .name("Food & Dining")
                .type(CategoryType.EXPENSE)
                .color("#FF6B6B")
                .icon("utensils")
                .build();

        testResponse = CategoryResponse.builder()
                .id(CATEGORY_ID)
                .name("Food & Dining")
                .type(CategoryType.EXPENSE)
                .color("#FF6B6B")
                .icon("utensils")
                .createdAt(Instant.now())
                .build();
    }

    // =========================================================================
    // getAllCategories()
    // =========================================================================

    @Test
    @DisplayName("getAllCategories: no type filter → returns all user's categories")
    void getAllCategories_withNoTypeFilter_shouldReturnAllCategories() {
        // Arrange
        List<Category> categories = List.of(testCategory);
        when(categoryRepository.findByUserIdOrderByNameAsc(USER_ID)).thenReturn(categories);
        when(categoryMapper.toResponse(testCategory)).thenReturn(testResponse);

        // Act
        List<CategoryResponse> result = categoryService.getAllCategories(USER_ID, null);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Food & Dining");

        // Verify the correct repository method was called (not the type-filtered one)
        verify(categoryRepository).findByUserIdOrderByNameAsc(USER_ID);
        verify(categoryRepository, never()).findByUserIdAndTypeOrderByNameAsc(any(), any());
    }

    @Test
    @DisplayName("getAllCategories: type=EXPENSE → uses type-filtered repository method")
    void getAllCategories_withTypeFilter_shouldCallTypeSpecificMethod() {
        // Arrange
        when(categoryRepository.findByUserIdAndTypeOrderByNameAsc(USER_ID, CategoryType.EXPENSE))
                .thenReturn(List.of(testCategory));
        when(categoryMapper.toResponse(testCategory)).thenReturn(testResponse);

        // Act
        List<CategoryResponse> result = categoryService.getAllCategories(USER_ID, CategoryType.EXPENSE);

        // Assert
        assertThat(result).hasSize(1);
        verify(categoryRepository).findByUserIdAndTypeOrderByNameAsc(USER_ID, CategoryType.EXPENSE);
        verify(categoryRepository, never()).findByUserIdOrderByNameAsc(any());
    }

    // =========================================================================
    // getCategoryById()
    // =========================================================================

    @Test
    @DisplayName("getCategoryById: owned category → returns response")
    void getCategoryById_whenOwned_shouldReturnCategoryResponse() {
        // Arrange
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID))
                .thenReturn(Optional.of(testCategory));
        when(categoryMapper.toResponse(testCategory)).thenReturn(testResponse);

        // Act
        CategoryResponse result = categoryService.getCategoryById(CATEGORY_ID, USER_ID);

        // Assert
        assertThat(result.getId()).isEqualTo(CATEGORY_ID);
        assertThat(result.getName()).isEqualTo("Food & Dining");
    }

    @Test
    @DisplayName("getCategoryById: not found (wrong user OR doesn't exist) → throws 404")
    void getCategoryById_whenNotOwned_shouldThrowResourceNotFoundException() {
        // findByIdAndUserId returns empty when:
        //   (a) category ID doesn't exist, OR
        //   (b) it belongs to a different user
        // Both cases return 404 — no information leakage about whether it exists.
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(CATEGORY_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================================
    // createCategory()
    // =========================================================================

    @Test
    @DisplayName("createCategory: unique name → saves and returns response")
    void createCategory_withUniqueName_shouldSaveAndReturn() {
        // Arrange
        CategoryRequest request = buildCategoryRequest("Groceries", CategoryType.EXPENSE);

        when(categoryRepository.existsByNameAndTypeAndUserId("Groceries", CategoryType.EXPENSE, USER_ID))
                .thenReturn(false);
        when(categoryMapper.toEntity(request)).thenReturn(testCategory);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(testUser);
        when(categoryRepository.save(testCategory)).thenReturn(testCategory);
        when(categoryMapper.toResponse(testCategory)).thenReturn(testResponse);

        // Act
        CategoryResponse result = categoryService.createCategory(request, USER_ID);

        // Assert
        assertThat(result).isNotNull();
        verify(categoryRepository).save(testCategory);
    }

    @Test
    @DisplayName("createCategory: duplicate name+type → throws DuplicateResourceException")
    void createCategory_withDuplicateName_shouldThrowConflict() {
        // Arrange
        CategoryRequest request = buildCategoryRequest("Food & Dining", CategoryType.EXPENSE);

        when(categoryRepository.existsByNameAndTypeAndUserId("Food & Dining", CategoryType.EXPENSE, USER_ID))
                .thenReturn(true);  // already exists

        // Act + Assert
        assertThatThrownBy(() -> categoryService.createCategory(request, USER_ID))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Food & Dining");

        // Nothing should be saved when name is duplicate
        verify(categoryRepository, never()).save(any());
    }

    // =========================================================================
    // deleteCategory()
    // =========================================================================

    @Test
    @DisplayName("deleteCategory: no transactions reference it → deletes successfully")
    void deleteCategory_whenNotInUse_shouldDelete() {
        // Arrange
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID))
                .thenReturn(Optional.of(testCategory));
        when(expenseRepository.existsByCategoryId(CATEGORY_ID)).thenReturn(false);
        when(incomeRepository.existsByCategoryId(CATEGORY_ID)).thenReturn(false);

        // Act
        categoryService.deleteCategory(CATEGORY_ID, USER_ID);

        // Assert
        verify(categoryRepository).delete(testCategory);
    }

    @Test
    @DisplayName("deleteCategory: expense references it → throws CategoryInUseException")
    void deleteCategory_whenUsedByExpense_shouldThrowCategoryInUseException() {
        // Arrange
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID))
                .thenReturn(Optional.of(testCategory));
        when(expenseRepository.existsByCategoryId(CATEGORY_ID)).thenReturn(true);  // in use!

        // Act + Assert
        assertThatThrownBy(() -> categoryService.deleteCategory(CATEGORY_ID, USER_ID))
                .isInstanceOf(CategoryInUseException.class)
                .hasMessageContaining("Food & Dining");

        // Verify the category was NOT deleted
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteCategory: income references it → throws CategoryInUseException")
    void deleteCategory_whenUsedByIncome_shouldThrowCategoryInUseException() {
        // Arrange — expenses=false but incomes=true → still blocked
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID))
                .thenReturn(Optional.of(testCategory));
        when(expenseRepository.existsByCategoryId(CATEGORY_ID)).thenReturn(false);
        when(incomeRepository.existsByCategoryId(CATEGORY_ID)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> categoryService.deleteCategory(CATEGORY_ID, USER_ID))
                .isInstanceOf(CategoryInUseException.class);

        verify(categoryRepository, never()).delete(any());
    }

    // =========================================================================
    // Test data builders
    // =========================================================================

    private CategoryRequest buildCategoryRequest(String name, CategoryType type) {
        CategoryRequest request = new CategoryRequest();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "type", type);
        ReflectionTestUtils.setField(request, "color", "#FF6B6B");
        ReflectionTestUtils.setField(request, "icon", "utensils");
        return request;
    }
}
