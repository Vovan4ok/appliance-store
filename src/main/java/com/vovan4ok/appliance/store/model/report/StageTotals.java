package com.vovan4ok.appliance.store.model.report;

import java.math.BigDecimal;

/**
 * Orders in one stage of the pipeline (cart, pending or approved) and what they add up to.
 *
 * @param value sum of unit price × quantity over all their rows
 */
public record StageTotals(int orders, long units, BigDecimal value) {
}
