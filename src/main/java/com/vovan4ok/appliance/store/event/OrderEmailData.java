package com.vovan4ok.appliance.store.event;

import com.vovan4ok.appliance.store.model.OrderRow;
import com.vovan4ok.appliance.store.model.Orders;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Immutable snapshot of an order, extracted while the JPA entities are still
 * attached so listeners can build emails safely after the transaction commits.
 */
@Getter
@RequiredArgsConstructor
public class OrderEmailData {

    private final Long orderId;
    private final String clientName;
    private final String clientEmail;
    private final BigDecimal totalAmount;
    private final List<Line> lines;

    public static OrderEmailData from(Orders order) {
        String name = order.getClient() != null ? order.getClient().getName() : null;
        String email = order.getClient() != null ? order.getClient().getEmail() : null;
        List<Line> lines = order.getOrderRowSet().stream()
                .map(OrderEmailData::toLine)
                .toList();
        return new OrderEmailData(order.getId(), name, email, order.getAmount(), lines);
    }

    private static Line toLine(OrderRow row) {
        String applianceName = row.getAppliance() != null ? row.getAppliance().getName() : "-";
        BigDecimal unitPrice = row.getAmount() != null ? row.getAmount() : BigDecimal.ZERO;
        Long quantity = row.getNumber() != null ? row.getNumber() : 0L;
        return new Line(applianceName, quantity, unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    @Getter
    @RequiredArgsConstructor
    public static class Line {
        private final String applianceName;
        private final Long quantity;
        private final BigDecimal lineTotal;
    }
}