package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.service.SalesReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Sales report for employees (SecurityConfig: /reports/** needs ROLE_EMPLOYEE). Read-only, so the read-only demo
 * accounts can open it too.
 */
@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final SalesReportService salesReportService;

    @GetMapping("/sales")
    public String sales(Model model) {
        model.addAttribute("report", salesReportService.report());
        model.addAttribute("topPerCategory", SalesReportService.TOP_PRODUCTS_PER_CATEGORY);
        return "report/sales";
    }
}
