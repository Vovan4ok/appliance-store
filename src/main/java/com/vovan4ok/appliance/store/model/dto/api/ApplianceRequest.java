package com.vovan4ok.appliance.store.model.dto.api;

import com.vovan4ok.appliance.store.model.Category;
import com.vovan4ok.appliance.store.model.PowerType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ApplianceRequest {

    @NotBlank
    private String name;

    private String model;

    @NotNull
    private Category category;

    @NotNull
    private PowerType powerType;

    @NotNull
    private Long manufacturerId;

    private String characteristic;
    private String description;
    private Integer power;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal price;

    @NotNull
    @Min(0)
    private Integer stock;
}