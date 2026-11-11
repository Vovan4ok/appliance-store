package com.vovan4ok.appliance.store.model.report;

import java.math.BigDecimal;

/**
 * Approved sales of one group (a category, a brand, ...).
 *
 * @param key          the group, e.g. a {@code Category} or a brand name (null: products without a brand)
 * @param orders       approved orders containing at least one product of the group
 * @param sharePercent the group's part of the revenue of all groups, 0..100 with one decimal
 */
public record RevenueShare<K>(K key, int orders, long units, BigDecimal revenue, BigDecimal sharePercent) {
}
