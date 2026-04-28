package com.vovan4ok.appliance.store.controller.api;

import com.vovan4ok.appliance.store.model.dto.api.OrderResponse;
import com.vovan4ok.appliance.store.model.dto.api.PageResponse;
import com.vovan4ok.appliance.store.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management")
public class OrderApiController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "List orders — EMPLOYEE sees all, CLIENT sees own orders")
    public PageResponse<OrderResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("id").descending());
        boolean isEmployee = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));

        if (isEmployee) {
            return PageResponse.from(orderService.findAll(pageable).map(OrderResponse::from));
        } else {
            return PageResponse.from(
                    orderService.findByClientEmail(authentication.getName(), pageable)
                            .map(OrderResponse::from));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public OrderResponse getById(@PathVariable Long id, Authentication authentication) {
        var order = orderService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

        boolean isEmployee = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));
        boolean isOwner = order.getClient() != null
                && order.getClient().getEmail().equals(authentication.getName());

        if (!isEmployee && !isOwner) {
            throw new IllegalArgumentException("Order not found: " + id);
        }
        return OrderResponse.from(order);
    }

    @PostMapping("/{id}/approve")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Approve an order (EMPLOYEE only)")
    public OrderResponse approve(@PathVariable Long id) {
        orderService.approve(id);
        return OrderResponse.from(orderService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id)));
    }
}