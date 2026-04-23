package com.vovan4ok.appliance.store.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Appliance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private Category category;

    private String model;

    @ManyToOne
    @JoinColumn(name = "manufacturer_id")
    private Manufacturer manufacturer;

    @Enumerated(EnumType.STRING)
    private PowerType powerType;

    private String characteristic;
    private String description;
    private Integer power;
    private BigDecimal price;
    private Integer stock;
}