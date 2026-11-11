package com.vovan4ok.appliance.store.model.report;

import com.vovan4ok.appliance.store.model.Category;

import java.util.List;

/**
 * Everything on the employees' sales report page. Revenue counts approved orders only.
 */
public record SalesReport(OrderPipeline pipeline,
                          List<RevenueShare<Category>> byCategory,
                          List<RevenueShare<String>> byBrand,
                          List<TopProduct> topProducts) {
}
