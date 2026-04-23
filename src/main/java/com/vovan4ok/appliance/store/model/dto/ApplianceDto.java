package com.vovan4ok.appliance.store.model.dto;

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

    @NotBlank
    private String name;

    @NotNull
    private Category category;

    @NotBlank
    private String model;

    @NotNull
    private Long manufacturerId;

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
}
