package com.fintrack.category;

import com.fintrack.common.BaseEntity;
import com.fintrack.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * JPA Entity: maps to the 'categories' table.
 *
 * Each category is owned by exactly one user — it's a personal category,
 * not shared across users.
 *
 * Unique constraint (user_id, name, type): a user can have a category named
 * "Food" for EXPENSE and another "Food" for INCOME — but not two EXPENSE "Food".
 * This matches real-world usage and is enforced at both DB and service layer.
 *
 * Relationship with User:
 *   MANY categories → ONE user  (Many-to-One)
 *   FetchType.LAZY: the User record is NOT loaded from DB when we load a Category,
 *   unless explicitly accessed. This avoids unnecessary DB joins.
 *
 * Why @JoinColumn?
 *   Tells JPA the FK column name in THIS table is 'user_id'.
 *   Without it, JPA would generate an ugly column name like 'user_id' anyway,
 *   but being explicit is best practice.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(
    name = "categories",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_category_user_name_type",
        columnNames = {"user_id", "name", "type"}
    )
)
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Many categories belong to one user.
     * LAZY fetch: don't load the full User row when we only need the category.
     * nullable = false: every category must have an owner.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 80)
    private String name;

    /**
     * INCOME or EXPENSE — stored as the enum name string ("INCOME").
     * EnumType.ORDINAL would store 0/1 — breaks if enum order changes.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CategoryType type;

    /**
     * Hex color code for UI rendering (e.g., "#F8C8DC").
     * Optional — defaults to a neutral color in the frontend if null.
     */
    @Column(length = 20)
    private String color;

    /**
     * Icon identifier (e.g., "food", "home", "car").
     * Optional — frontend maps this to an icon component.
     */
    @Column(length = 40)
    private String icon;
}
