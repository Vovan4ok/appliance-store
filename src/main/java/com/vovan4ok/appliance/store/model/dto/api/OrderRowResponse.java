package com.vovan4ok.appliance.store.model.dto.api;

import com.vovan4ok.appliance.store.model.OrderRow;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class OrderRowResponse {

    private Long id;
    private Long applianceId;
    private String applianceName;
    private BigDecimal unitPrice;
    private Long quantity;
    private BigDecimal subtotal;

    public static OrderRowResponse from(OrderRow row) {
        BigDecimal subtotal = (row.getAmount() != null && row.getNumber() != null)
                ? row.getAmount().multiply(BigDecimal.valueOf(row.getNumber()))
                : BigDecimal.ZERO;
        return OrderRowResponse.builder()
                .id(row.getId())
                .applianceId(row.getAppliance() != null ? row.getAppliance().getId() : null)
                .applianceName(row.getAppliance() != null ? row.getAppliance().getName() : null)
                .unitPrice(row.getAmount())
                .quantity(row.getNumber())
                .subtotal(subtotal)
                .build();
    }
}