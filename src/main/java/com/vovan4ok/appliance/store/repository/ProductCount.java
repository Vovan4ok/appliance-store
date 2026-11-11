package com.vovan4ok.appliance.store.repository;

/**
 * Query result: a product id with a count (orders, units sold...).
 */
public record ProductCount(Long applianceId, Long count) {
}
