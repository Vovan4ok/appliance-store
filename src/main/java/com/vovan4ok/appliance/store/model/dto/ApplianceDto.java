package com.vovan4ok.appliance.store.model.dto;

import com.vovan4ok.appliance.store.model.Appliance;
import com.vovan4ok.appliance.store.model.Category;
import com.vovan4ok.appliance.store.model.PowerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ApplianceDto {

    private Long id;

    @NotBlank
    private String name;

    @NotNull
    private Category category;

    @NotBlank
    private String model;

    @NotNull
    private Long manufacturerId;

    private String manufacturerName;

    @NotNull
    private PowerType powerType;

    @NotBlank
    private String characteristic;

    @NotBlank
    private String description;

    @NotNull
    private Integer power;

    @NotNull
    private BigDecimal price;

    @NotNull
    private Integer stock;

    public static ApplianceDto from(Appliance a) {
        ApplianceDto dto = new ApplianceDto();
        dto.setId(a.getId());
        dto.setName(a.getName());
        dto.setCategory(a.getCategory());
        dto.setModel(a.getModel());
        dto.setManufacturerId(a.getManufacturer() != null ? a.getManufacturer().getId() : null);
        dto.setManufacturerName(a.getManufacturer() != null ? a.getManufacturer().getName() : null);
        dto.setPowerType(a.getPowerType());
        dto.setCharacteristic(a.getCharacteristic());
        dto.setDescription(a.getDescription());
        dto.setPower(a.getPower());
        dto.setPrice(a.getPrice());
        dto.setStock(a.getStock());
        return dto;
    }
}
