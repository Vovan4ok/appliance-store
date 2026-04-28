package com.vovan4ok.appliance.store.model.dto;

import com.vovan4ok.appliance.store.model.Employee;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class EmployeeViewDto {

    private Long id;
    private String name;
    private String email;
    private String department;
    private String phone;
    private LocalDate dateOfBirth;

    public static EmployeeViewDto from(Employee e) {
        EmployeeViewDto dto = new EmployeeViewDto();
        dto.setId(e.getId());
        dto.setName(e.getName());
        dto.setEmail(e.getEmail());
        dto.setDepartment(e.getDepartment());
        dto.setPhone(e.getPhone());
        dto.setDateOfBirth(e.getDateOfBirth());
        return dto;
    }
}