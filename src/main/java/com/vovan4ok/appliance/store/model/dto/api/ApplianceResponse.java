package com.vovan4ok.appliance.store.model.dto.api;

import com.vovan4ok.appliance.store.model.Appliance;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ApplianceResponse {

    private Long id;
    private String name;
    private String model;
    private String category;
    private String powerType;
    private ManufacturerResponse manufacturer;
    private String characteristic;
    private String description;
    private Integer power;
    private BigDecimal price;
    private Integer stock;

    public static ApplianceResponse from(Appliance a) {
        return ApplianceResponse.builder()
                .id(a.getId())
                .name(a.getName())
                .model(a.getModel())
                .category(a.getCategory() != null ? a.getCategory().name() : null)
                .powerType(a.getPowerType() != null ? a.getPowerType().name() : null)
                .manufacturer(a.getManufacturer() != null ? ManufacturerResponse.from(a.getManufacturer()) : null)
                .characteristic(a.getCharacteristic())
                .description(a.getDescription())
                .power(a.getPower())
                .price(a.getPrice())
                .stock(a.getStock())
                .build();
    }
}