package com.vovan4ok.appliance.store.service;

import com.vovan4ok.appliance.store.model.report.OrderPipeline;
import com.vovan4ok.appliance.store.model.report.RevenueShare;
import com.vovan4ok.appliance.store.model.report.SalesReport;
import com.vovan4ok.appliance.store.model.report.StageTotals;
import com.vovan4ok.appliance.store.model.report.TopProduct;
import com.vovan4ok.appliance.store.repository.SalesReportRepository;
import com.vovan4ok.appliance.store.service.impl.SalesReportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static com.vovan4ok.appliance.store.model.Category.BIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesReportServiceImplTest {

    @Mock
    SalesReportRepository repository;

    @InjectMocks
    SalesReportServiceImpl service;

    @Test
    void report_combinesTheFourQueries_withTheConfiguredTopSize() {
        StageTotals none = new StageTotals(0, 0, BigDecimal.ZERO);
        OrderPipeline pipeline = new OrderPipeline(none, none, new StageTotals(1, 2, BigDecimal.TEN));
        var byCategory = List.of(new RevenueShare<>(BIG, 1, 2, BigDecimal.TEN, new BigDecimal("100.0")));
        var byBrand = List.of(new RevenueShare<>("Bosch", 1, 2, BigDecimal.TEN, new BigDecimal("100.0")));
        var top = List.of(new TopProduct(BIG, 1, 27, "Freestanding Dishwasher", 2, BigDecimal.TEN));
        when(repository.orderPipeline()).thenReturn(pipeline);
        when(repository.revenueByCategory()).thenReturn(byCategory);
        when(repository.revenueByBrand()).thenReturn(byBrand);
        when(repository.topProductsPerCategory(SalesReportService.TOP_PRODUCTS_PER_CATEGORY)).thenReturn(top);

        assertThat(service.report()).isEqualTo(new SalesReport(pipeline, byCategory, byBrand, top));
    }
}
