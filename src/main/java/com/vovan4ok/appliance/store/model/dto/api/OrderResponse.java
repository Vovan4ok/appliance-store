package com.vovan4ok.appliance.store.model.dto.api;

import com.vovan4ok.appliance.store.model.Orders;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class OrderResponse {

    private Long id;
    private Boolean approved;
    private Long clientId;
    private String clientName;
    private Long employeeId;
    private String employeeName;
    private List<OrderRowResponse> items;
    private BigDecimal total;

    public static OrderResponse from(Orders o) {
        List<OrderRowResponse> items = o.getOrderRowSet().stream()
                .map(OrderRowResponse::from)
                .collect(Collectors.toList());
        return OrderResponse.builder()
                .id(o.getId())
                .approved(o.getApproved())
                .clientId(o.getClient() != null ? o.getClient().getId() : null)
                .clientName(o.getClient() != null ? o.getClient().getName() : null)
                .employeeId(o.getEmployee() != null ? o.getEmployee().getId() : null)
                .employeeName(o.getEmployee() != null ? o.getEmployee().getName() : null)
                .items(items)
                .total(o.getAmount())
                .build();
    }
}