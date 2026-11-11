package com.vovan4ok.appliance.store.service;

import com.vovan4ok.appliance.store.model.report.SalesReport;

public interface SalesReportService {

    /** Best sellers listed per category on the report (more when products tie). */
    int TOP_PRODUCTS_PER_CATEGORY = 3;

    SalesReport report();
}
