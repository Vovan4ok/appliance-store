package com.vovan4ok.appliance.store.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Boolean approved;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private Set<OrderRow> orderRowSet = new HashSet<>();

    public BigDecimal getAmount() {
        if (orderRowSet == null || orderRowSet.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return orderRowSet.stream()
                .filter(row -> row.getAmount() != null && row.getNumber() != null)
                .map(row -> row.getAmount().multiply(BigDecimal.valueOf(row.getNumber())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}