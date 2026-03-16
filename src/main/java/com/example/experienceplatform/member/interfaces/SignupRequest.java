package com.example.experienceplatform.member.interfaces;

import com.example.experienceplatform.member.application.RegisterMemberCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SignupRequest {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(
            regexp = "^(?=.*[a-zA-Z])(?=.*[\\d!@#$%^&*])[a-zA-Z\\d!@#$%^&*]{8,}$",
            message = "비밀번호는 8자 이상, 영문과 숫자/특수문자를 포함해야 합니다."
    )
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수입니다.")
    private String passwordConfirm;

    @NotBlank(message = "닉네임은 필수입니다.")
    @Pattern(
            regexp = "^[가-힣a-zA-Z0-9]{2,20}$",
            message = "닉네임은 2~20자의 한글, 영문, 숫자만 사용할 수 있습니다."
    )
    private String nickname;

    public SignupRequest() {
    }

    public SignupRequest(String email, String password, String passwordConfirm, String nickname) {
        this.email = email;
        this.password = password;
        this.passwordConfirm = passwordConfirm;
        this.nickname = nickname;
    }

    public RegisterMemberCommand toCommand() {
        return new RegisterMemberCommand(email, password, passwordConfirm, nickname);
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public String getNickname() {
        return nickname;
    }
}
