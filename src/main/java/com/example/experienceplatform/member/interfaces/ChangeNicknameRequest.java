package com.example.experienceplatform.member.interfaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ChangeNicknameRequest {

    @NotBlank(message = "닉네임은 필수입니다.")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9]{2,20}$",
            message = "닉네임은 2~20자의 한글, 영문, 숫자만 사용할 수 있습니다.")
    private String nickname;

    protected ChangeNicknameRequest() {
    }

    public ChangeNicknameRequest(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }
}
