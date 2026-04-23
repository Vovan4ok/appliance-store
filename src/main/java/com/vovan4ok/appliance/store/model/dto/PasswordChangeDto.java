package com.vovan4ok.appliance.store.model.dto;

import com.vovan4ok.appliance.store.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PasswordChangeDto {

    @NotBlank(message = "{user.password.old.blank}")
    private String oldPassword;

    @NotBlank(message = "{user.password.is.correctly}")
    @ValidPassword
    private String newPassword;

    @NotBlank(message = "{user.password.confirm.blank}")
    private String confirmPassword;
}