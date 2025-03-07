package com.intallysh.widom.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {

    @NotBlank(message = "Username is mandatory")
    private String username;

    @Pattern(regexp = "^(?=.*[a-z]|[A-Z]|[0-9])(?=\\S+$).{8,}$", message = "Please Enter a valid Password")
    @NotBlank(message = "Password is mandatory")
    private String password;
}
