package com.fintrack.category;

/**
 * Discriminates whether a category is for income or expense transactions.
 *
 * Stored as VARCHAR (EnumType.STRING) in the 'categories' table.
 * DB CHECK constraint enforces only 'INCOME' or 'EXPENSE' values.
 *
 * A category with type INCOME should only be assigned to income records.
 * A category with type EXPENSE should only be assigned to expense records.
 * This is validated at the service layer.
 */
public enum CategoryType {
    INCOME,
    EXPENSE
}
