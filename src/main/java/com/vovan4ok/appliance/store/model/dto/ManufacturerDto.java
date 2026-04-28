package com.vovan4ok.appliance.store.model.dto;

import com.vovan4ok.appliance.store.model.Manufacturer;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ManufacturerDto {

    private Long id;

    @NotBlank
    private String name;
    private String country;
    private String website;
    private String description;
    private Integer foundedYear;
    private String logoPath;

    public static ManufacturerDto from(Manufacturer m) {
        ManufacturerDto dto = new ManufacturerDto();
        dto.setId(m.getId());
        dto.setName(m.getName());
        dto.setCountry(m.getCountry());
        dto.setWebsite(m.getWebsite());
        dto.setDescription(m.getDescription());
        dto.setFoundedYear(m.getFoundedYear());
        dto.setLogoPath(m.getLogoPath());
        return dto;
    }
}