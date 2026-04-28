package com.vovan4ok.appliance.store.model.dto.api;

import com.vovan4ok.appliance.store.model.Manufacturer;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ManufacturerResponse {

    private Long id;
    private String name;
    private String country;
    private String website;
    private String description;
    private String logoPath;
    private Integer foundedYear;

    public static ManufacturerResponse from(Manufacturer m) {
        return ManufacturerResponse.builder()
                .id(m.getId())
                .name(m.getName())
                .country(m.getCountry())
                .website(m.getWebsite())
                .description(m.getDescription())
                .logoPath(m.getLogoPath())
                .foundedYear(m.getFoundedYear())
                .build();
    }
}