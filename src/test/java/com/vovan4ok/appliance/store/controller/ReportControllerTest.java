package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.config.SecurityConfig;
import com.vovan4ok.appliance.store.model.report.OrderPipeline;
import com.vovan4ok.appliance.store.model.report.RevenueShare;
import com.vovan4ok.appliance.store.model.report.SalesReport;
import com.vovan4ok.appliance.store.model.report.StageTotals;
import com.vovan4ok.appliance.store.model.report.TopProduct;
import com.vovan4ok.appliance.store.security.TokenDenylist;
import com.vovan4ok.appliance.store.service.SalesReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static com.vovan4ok.appliance.store.model.Category.BIG;
import static com.vovan4ok.appliance.store.model.Category.SMALL;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * The sales report page in the real security chain: employees (also the read-only demo ones) see it, clients don't.
 */
@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.demo.read-only-accounts=phobos@gmail.com")
class ReportControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SalesReportService salesReportService;
    @MockitoBean
    UserDetailsService userDetailsService;
    @MockitoBean
    TokenDenylist tokenDenylist;

    private static final SalesReport REPORT = new SalesReport(
            new OrderPipeline(
                    new StageTotals(2, 8, new BigDecimal("1919.92")),
                    new StageTotals(3, 10, new BigDecimal("6267.92")),
                    new StageTotals(3, 11, new BigDecimal("6804.89"))),
            List.of(new RevenueShare<>(BIG, 3, 6, new BigDecimal("5549.94"), new BigDecimal("81.6")),
                    new RevenueShare<>(SMALL, 2, 5, new BigDecimal("1254.95"), new BigDecimal("18.4"))),
            List.of(new RevenueShare<>("Samsung", 2, 3, new BigDecimal("2799.97"), new BigDecimal("41.1")),
                    new RevenueShare<>(null, 1, 1, new BigDecimal("10.00"), new BigDecimal("0.1"))),
            List.of(new TopProduct(BIG, 1, 8, "Side-by-Side Refrigerator", 1, new BigDecimal("1299.99"))));

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_seesTheReport() throws Exception {
        when(salesReportService.report()).thenReturn(REPORT);

        mockMvc.perform(get("/reports/sales"))
                .andExpect(status().isOk())
                .andExpect(view().name("report/sales"))
                .andExpect(model().attribute("report", REPORT))
                .andExpect(content().string(containsString("6804.89")))
                .andExpect(content().string(containsString("81.6 %")))
                .andExpect(content().string(containsString("Samsung")))
                .andExpect(content().string(containsString("No brand")))
                .andExpect(content().string(containsString("Side-by-Side Refrigerator")))
                .andExpect(content().string(containsString("Top 3 per category")));
    }

    @Test
    @WithMockUser(username = "phobos@gmail.com", roles = "EMPLOYEE")
    void readOnlyDemoEmployee_canOpenIt() throws Exception {
        when(salesReportService.report()).thenReturn(REPORT);

        mockMvc.perform(get("/reports/sales")).andExpect(status().isOk());
    }

    /** MVC pages answer "access denied" with a redirect to the login page (SecurityConfig.jsonForApiElseLogin). */
    @Test
    @WithMockUser(roles = "CLIENT")
    void client_cannotSeeIt() throws Exception {
        mockMvc.perform(get("/reports/sales")).andExpect(redirectedUrl("/login"));

        verify(salesReportService, never()).report();
    }

    @Test
    void anonymous_isSentToLogin() throws Exception {
        mockMvc.perform(get("/reports/sales")).andExpect(redirectedUrl("/login"));

        verify(salesReportService, never()).report();
    }
}
