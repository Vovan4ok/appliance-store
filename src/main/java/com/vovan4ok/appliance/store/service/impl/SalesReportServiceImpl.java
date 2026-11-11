package com.vovan4ok.appliance.store.service.impl;

import com.vovan4ok.appliance.store.model.report.SalesReport;
import com.vovan4ok.appliance.store.repository.SalesReportRepository;
import com.vovan4ok.appliance.store.service.SalesReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SalesReportServiceImpl implements SalesReportService {

    private final SalesReportRepository salesReportRepository;

    /** One read-only transaction (one connection) for the four queries of the page. */
    @Transactional(readOnly = true)
    @Override
    public SalesReport report() {
        return new SalesReport(
                salesReportRepository.orderPipeline(),
                salesReportRepository.revenueByCategory(),
                salesReportRepository.revenueByBrand(),
                salesReportRepository.topProductsPerCategory(TOP_PRODUCTS_PER_CATEGORY));
    }
}
