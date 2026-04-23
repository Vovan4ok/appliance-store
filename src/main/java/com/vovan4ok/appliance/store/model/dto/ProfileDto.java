package com.vovan4ok.appliance.store.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class ProfileDto {

    @NotBlank(message = "{user.name.is.mandatory}")
    private String name;

    @Pattern(regexp = "^$|^\\+?(?:\\d[\\s\\-()]*){7,15}$", message = "{user.phone.error}")
    private String phone;

    @Past(message = "{user.dob.error}")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    private String card;

    private String department;
}