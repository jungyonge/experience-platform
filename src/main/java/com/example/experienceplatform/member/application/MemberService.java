package com.example.experienceplatform.member.application;

import com.example.experienceplatform.member.domain.*;
import com.example.experienceplatform.member.domain.exception.DuplicateEmailException;
import com.example.experienceplatform.member.domain.exception.DuplicateNicknameException;
import com.example.experienceplatform.member.domain.exception.InvalidPasswordException;
import com.example.experienceplatform.member.domain.exception.PasswordMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@Transactional
public class MemberService {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-zA-Z])(?=.*[\\d!@#$%^&*])[a-zA-Z\\d!@#$%^&*]{8,}$");

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public MemberInfo registerMember(RegisterMemberCommand command) {
        validatePassword(command.getPassword(), command.getPasswordConfirm());

        String encodedPassword = passwordEncoder.encode(command.getPassword());

        Member member = new Member(
                new Email(command.getEmail()),
                new PasswordHash(encodedPassword),
                new Nickname(command.getNickname())
        );

        try {
            Member savedMember = memberRepository.saveAndFlush(member);
            return MemberInfo.from(savedMember);
        } catch (DataIntegrityViolationException e) {
            handleDuplicateException(e);
            throw e;
        }
    }

    private void validatePassword(String password, String passwordConfirm) {
        if (!password.equals(passwordConfirm)) {
            throw new PasswordMismatchException();
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new InvalidPasswordException();
        }
    }

    private void handleDuplicateException(DataIntegrityViolationException e) {
        String message = (e.getRootCause() != null)
                ? e.getRootCause().getMessage()
                : e.getMessage();

        if (message != null) {
            String constraintInfo = message.toUpperCase().split("SQL STATEMENT")[0];
            if (constraintInfo.contains("EMAIL")) {
                throw new DuplicateEmailException();
            }
            if (constraintInfo.contains("NICKNAME")) {
                throw new DuplicateNicknameException();
            }
        }
    }
}
