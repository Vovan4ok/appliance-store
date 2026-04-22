package com.vovan4ok.appliance.store.model;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Client extends User {

    private String card;

    public Client() {
    }

    public Client(Long id, String name, String email, String password, String card) {
        super(id, name, email, password);
        this.card = card;
    }
}