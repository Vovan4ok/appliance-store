package com.vovan4ok.appliance.store.model.dto.api;

import com.vovan4ok.appliance.store.model.Manufacturer;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ManufacturerResponse {

    private Long id;
    private String name;

    public static ManufacturerResponse from(Manufacturer m) {
        return ManufacturerResponse.builder()
                .id(m.getId())
                .name(m.getName())
                .build();
    }
}