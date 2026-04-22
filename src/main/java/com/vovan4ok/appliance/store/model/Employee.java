package com.vovan4ok.appliance.store.model;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Employee extends User {

    private String department;

    public Employee() {
    }

    public Employee(Long id, String name, String email, String password, String department) {
        super(id, name, email, password);
        this.department = department;
    }
}