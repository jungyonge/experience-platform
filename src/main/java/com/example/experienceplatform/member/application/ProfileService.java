package com.example.experienceplatform.member.application;

import com.example.experienceplatform.member.domain.*;
import com.example.experienceplatform.member.domain.exception.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class ProfileService {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-zA-Z])(?=.*[\\d!@#$%^&*])[a-zA-Z\\d!@#$%^&*]{8,}$");

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(MemberRepository memberRepository,
                          RefreshTokenRepository refreshTokenRepository,
                          PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public MemberProfile getProfile(Long memberId) {
        Member member = findMember(memberId);
        return MemberProfile.from(member);
    }

    @Transactional
    public MemberProfile changeNickname(ChangeNicknameCommand command) {
        Member member = findMember(command.getMemberId());

        Nickname newNickname = new Nickname(command.getNewNickname());

        // 현재와 동일하면 변경 없이 성공
        if (member.getNickname().equals(newNickname)) {
            return MemberProfile.from(member);
        }

        // 중복 사전 검사
        if (memberRepository.existsByNickname(newNickname)) {
            throw new DuplicateNicknameException();
        }

        member.changeNickname(newNickname);

        try {
            memberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateNicknameException();
        }

        return MemberProfile.from(member);
    }

    @Transactional
    public void changePassword(ChangePasswordCommand command) {
        Member member = findMember(command.getMemberId());

        // 새 비밀번호 확인 일치 검증
        if (!command.getNewPassword().equals(command.getNewPasswordConfirm())) {
            throw new PasswordMismatchException();
        }

        // 새 비밀번호 정책 검증
        if (!PASSWORD_PATTERN.matcher(command.getNewPassword()).matches()) {
            throw new InvalidPasswordException();
        }

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(command.getCurrentPassword(), member.getPasswordHash().getValue())) {
            throw new CurrentPasswordMismatchException();
        }

        // 현재/새 비밀번호 동일 검사
        if (passwordEncoder.matches(command.getNewPassword(), member.getPasswordHash().getValue())) {
            throw new SamePasswordException();
        }

        PasswordHash newPasswordHash = new PasswordHash(passwordEncoder.encode(command.getNewPassword()));
        member.changePassword(newPasswordHash);
        memberRepository.save(member);
    }

    @Transactional
    public void withdraw(WithdrawMemberCommand command) {
        Member member = findMember(command.getMemberId());

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(command.getCurrentPassword(), member.getPasswordHash().getValue())) {
            throw new CurrentPasswordMismatchException();
        }

        member.withdraw();
        memberRepository.save(member);

        // Refresh Token 삭제
        refreshTokenRepository.deleteByMemberId(command.getMemberId());
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
    }
}
