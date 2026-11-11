package com.vovan4ok.appliance.store.model.report;

/**
 * Where all orders are right now: still in carts (approved = false), submitted and waiting for an employee
 * (approved = null), or approved (approved = true).
 */
public record OrderPipeline(StageTotals carts, StageTotals pending, StageTotals approved) {
}
