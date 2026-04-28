package com.vovan4ok.appliance.store.model.dto.api;

import com.vovan4ok.appliance.store.model.Client;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ClientResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String card;

    public static ClientResponse from(Client c) {
        return ClientResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .email(c.getEmail())
                .phone(c.getPhone())
                .dateOfBirth(c.getDateOfBirth())
                .card(c.getCard())
                .build();
    }
}