package com.example.experienceplatform.member.application;

import com.example.experienceplatform.member.domain.*;
import com.example.experienceplatform.member.domain.exception.AccountDisabledException;
import com.example.experienceplatform.member.domain.exception.AuthenticationFailedException;
import com.example.experienceplatform.member.domain.exception.InvalidRefreshTokenException;
import com.example.experienceplatform.member.domain.exception.RefreshTokenExpiredException;
import com.example.experienceplatform.member.infrastructure.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthService(MemberRepository memberRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtTokenProvider jwtTokenProvider,
                       PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthTokenInfo login(LoginCommand command) {
        Member member = memberRepository.findByEmail(new Email(command.getEmail()))
                .orElseThrow(AuthenticationFailedException::new);

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new AccountDisabledException();
        }

        if (!passwordEncoder.matches(command.getPassword(), member.getPasswordHash().getValue())) {
            throw new AuthenticationFailedException();
        }

        String accessToken = jwtTokenProvider.createAccessToken(
                member.getId(), member.getEmail().getValue());
        String refreshTokenValue = jwtTokenProvider.createRefreshToken();

        saveOrRotateRefreshToken(member.getId(), refreshTokenValue);

        return new AuthTokenInfo(
                accessToken,
                refreshTokenValue,
                jwtTokenProvider.getAccessTokenExpirySeconds());
    }

    public AuthTokenInfo refresh(RefreshTokenCommand command) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(command.getRefreshToken())
                .orElseThrow(InvalidRefreshTokenException::new);

        if (refreshToken.isExpired()) {
            refreshTokenRepository.deleteByMemberId(refreshToken.getMemberId());
            throw new RefreshTokenExpiredException();
        }

        Member member = memberRepository.findById(refreshToken.getMemberId())
                .orElseThrow(InvalidRefreshTokenException::new);

        String newAccessToken = jwtTokenProvider.createAccessToken(
                member.getId(), member.getEmail().getValue());
        String newRefreshTokenValue = jwtTokenProvider.createRefreshToken();

        // Refresh Token Rotation
        refreshToken.rotate(
                newRefreshTokenValue,
                LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiryMillis() / 1000));
        refreshTokenRepository.save(refreshToken);

        return new AuthTokenInfo(
                newAccessToken,
                newRefreshTokenValue,
                jwtTokenProvider.getAccessTokenExpirySeconds());
    }

    public void logout(Long memberId) {
        refreshTokenRepository.deleteByMemberId(memberId);
    }

    private void saveOrRotateRefreshToken(Long memberId, String tokenValue) {
        LocalDateTime expiryDate = LocalDateTime.now()
                .plusSeconds(jwtTokenProvider.getRefreshTokenExpiryMillis() / 1000);

        Optional<RefreshToken> existing = refreshTokenRepository.findByMemberId(memberId);

        if (existing.isPresent()) {
            existing.get().rotate(tokenValue, expiryDate);
            refreshTokenRepository.save(existing.get());
        } else {
            refreshTokenRepository.save(new RefreshToken(memberId, tokenValue, expiryDate));
        }
    }
}
