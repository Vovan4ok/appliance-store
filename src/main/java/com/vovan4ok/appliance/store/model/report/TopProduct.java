package com.vovan4ok.appliance.store.model.report;

import com.vovan4ok.appliance.store.model.Category;

import java.math.BigDecimal;

/**
 * A best seller within its category.
 *
 * @param rank 1 = most units sold in the category; products with the same units and revenue share a rank
 */
public record TopProduct(Category category, int rank, long applianceId, String name, long units,
                         BigDecimal revenue) {
}
