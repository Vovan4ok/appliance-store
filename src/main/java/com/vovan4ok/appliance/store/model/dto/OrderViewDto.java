package com.vovan4ok.appliance.store.model.dto;

import com.vovan4ok.appliance.store.model.Orders;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class OrderViewDto {

    private Long id;
    private String clientName;
    private String employeeName;
    private Boolean approved;
    private BigDecimal amount;
    private Set<OrderRowViewDto> rows = new LinkedHashSet<>();

    public static OrderViewDto from(Orders o) {
        OrderViewDto dto = new OrderViewDto();
        dto.setId(o.getId());
        dto.setClientName(o.getClient() != null ? o.getClient().getName() : null);
        dto.setEmployeeName(o.getEmployee() != null ? o.getEmployee().getName() : null);
        dto.setApproved(o.getApproved());
        dto.setAmount(o.getAmount());
        dto.setRows(o.getOrderRowSet().stream()
                .map(OrderRowViewDto::from)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        return dto;
    }
}