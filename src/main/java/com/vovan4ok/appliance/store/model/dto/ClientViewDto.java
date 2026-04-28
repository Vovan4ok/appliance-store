package com.vovan4ok.appliance.store.model.dto;

import com.vovan4ok.appliance.store.model.Client;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class ClientViewDto {

    private Long id;
    private String name;
    private String email;
    private String card;
    private String phone;
    private LocalDate dateOfBirth;

    public static ClientViewDto from(Client c) {
        ClientViewDto dto = new ClientViewDto();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setEmail(c.getEmail());
        dto.setCard(c.getCard());
        dto.setPhone(c.getPhone());
        dto.setDateOfBirth(c.getDateOfBirth());
        return dto;
    }
}