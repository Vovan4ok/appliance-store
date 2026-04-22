package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.model.Appliance;
import com.vovan4ok.appliance.store.model.Category;
import com.vovan4ok.appliance.store.model.Client;
import com.vovan4ok.appliance.store.model.Orders;
import com.vovan4ok.appliance.store.model.PowerType;
import com.vovan4ok.appliance.store.service.ApplianceService;
import com.vovan4ok.appliance.store.service.ClientService;
import com.vovan4ok.appliance.store.service.ManufacturerService;
import com.vovan4ok.appliance.store.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@Controller
@RequestMapping("/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ApplianceService applianceService;
    private final OrderService orderService;
    private final ClientService clientService;
    private final ManufacturerService manufacturerService;

    @GetMapping
    public String catalog(Model model,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "6") int size,
                          @RequestParam(required = false) String name,
                          @RequestParam(required = false) Category category,
                          @RequestParam(required = false) PowerType powerType,
                          @RequestParam(required = false) Long manufacturerId,
                          @RequestParam(required = false) BigDecimal minPrice,
                          @RequestParam(required = false) BigDecimal maxPrice,
                          @RequestParam(defaultValue = "name") String sortBy,
                          @RequestParam(defaultValue = "asc") String sortDir,
                          Authentication auth) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Page<Appliance> result = applianceService.findAll(
                name, category, powerType, manufacturerId, minPrice, maxPrice,
                PageRequest.of(page, size, sort)
        );

        model.addAttribute("appliances", result.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", result.getTotalPages());

        model.addAttribute("filterName", name);
        model.addAttribute("filterCategory", category);
        model.addAttribute("filterPowerType", powerType);
        model.addAttribute("filterManufacturerId", manufacturerId);
        model.addAttribute("filterMinPrice", minPrice);
        model.addAttribute("filterMaxPrice", maxPrice);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        model.addAttribute("categories", Category.values());
        model.addAttribute("powerTypes", PowerType.values());
        model.addAttribute("manufacturers", manufacturerService.findAll());

        if (auth != null) {
            int cartCount = orderService.findPendingByClientEmail(auth.getName())
                    .map(o -> o.getOrderRowSet().size())
                    .orElse(0);
            model.addAttribute("cartCount", cartCount);
        }

        log.debug("GET /shop page={} size={} name={} category={} powerType={} manufacturerId={} sortBy={} sortDir={}",
                page, size, name, category, powerType, manufacturerId, sortBy, sortDir);

        return "shop/shop";
    }

    @PostMapping("/add-to-cart")
    public String addToCart(@RequestParam Long applianceId,
                            @RequestParam(defaultValue = "1") Long quantity,
                            Authentication auth) {
        Appliance appliance = applianceService.findById(applianceId)
                .orElseThrow(() -> new IllegalArgumentException("Appliance not found: " + applianceId));

        Orders order = orderService.findPendingByClientEmail(auth.getName())
                .orElseGet(() -> {
                    Client client = clientService.findByEmail(auth.getName())
                            .orElseThrow(() -> new IllegalStateException("Client not found: " + auth.getName()));
                    Orders o = new Orders();
                    o.setClient(client);
                    o.setApproved(false);
                    return orderService.save(o);
                });

        orderService.addOrderRow(order.getId(), applianceId, quantity, appliance.getPrice());
        log.info("Client {} added appliance id={} x{} to cart (order id={})",
                auth.getName(), applianceId, quantity, order.getId());
        return "redirect:/shop";
    }

    @PostMapping("/checkout")
    public String checkout(@RequestParam Long orderId) {
        log.info("Client submitting cart as order id={}", orderId);
        orderService.submitCart(orderId);
        return "redirect:/orders/" + orderId + "/edit";
    }

    @GetMapping("/cart")
    public String cart(Authentication auth, Model model) {
        log.debug("GET /shop/cart for {}", auth != null ? auth.getName() : "anonymous");
        if (auth != null) {
            Orders order = orderService.findPendingByClientEmail(auth.getName()).orElse(null);
            model.addAttribute("order", order);
        }
        return "shop/cart";
    }

    @PostMapping("/cart/rows/{rowId}/update")
    public String updateCartRow(@PathVariable Long rowId,
                                @RequestParam Long number) {
        log.info("Updating cart row id={} quantity={}", rowId, number);
        orderService.updateOrderRowQuantity(rowId, number);
        return "redirect:/shop/cart";
    }

    @PostMapping("/cart/rows/{rowId}/remove")
    public String removeCartRow(@PathVariable Long rowId) {
        log.info("Removing cart row id={}", rowId);
        orderService.removeOrderRow(rowId);
        return "redirect:/shop/cart";
    }

    @PostMapping("/cancel")
    public String cancelOrder(@RequestParam Long orderId) {
        log.info("Cancelling order id={}", orderId);
        orderService.delete(orderId);
        return "redirect:/shop";
    }
}