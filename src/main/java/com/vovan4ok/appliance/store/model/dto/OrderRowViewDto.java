package com.vovan4ok.appliance.store.model.dto;

import com.vovan4ok.appliance.store.model.OrderRow;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class OrderRowViewDto {

    private Long id;
    private String applianceName;
    private Integer applianceStock;
    private String manufacturerName;
    private Long number;
    private BigDecimal amount;

    public static OrderRowViewDto from(OrderRow row) {
        OrderRowViewDto dto = new OrderRowViewDto();
        dto.setId(row.getId());
        if (row.getAppliance() != null) {
            dto.setApplianceName(row.getAppliance().getName());
            dto.setApplianceStock(row.getAppliance().getStock());
            if (row.getAppliance().getManufacturer() != null) {
                dto.setManufacturerName(row.getAppliance().getManufacturer().getName());
            }
        }
        dto.setNumber(row.getNumber());
        dto.setAmount(row.getAmount());
        return dto;
    }
}