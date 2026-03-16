package com.example.experienceplatform.member.application;

public class RegisterMemberCommand {

    private final String email;
    private final String password;
    private final String passwordConfirm;
    private final String nickname;

    public RegisterMemberCommand(String email, String password, String passwordConfirm, String nickname) {
        this.email = email;
        this.password = password;
        this.passwordConfirm = passwordConfirm;
        this.nickname = nickname;
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
