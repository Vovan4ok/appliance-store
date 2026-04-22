package com.vovan4ok.appliance.store.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderDto {

    @NotNull
    private Long clientId;

    @NotNull
    private Long employeeId;
}
