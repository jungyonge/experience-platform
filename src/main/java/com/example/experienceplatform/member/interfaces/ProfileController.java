package com.example.experienceplatform.member.interfaces;

import com.example.experienceplatform.member.application.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members/me")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profile")
    public ResponseEntity<MemberProfileResponse> getProfile(
            @AuthenticationPrincipal AuthenticatedMember principal) {
        MemberProfile profile = profileService.getProfile(principal.getMemberId());
        return ResponseEntity.ok(MemberProfileResponse.from(profile));
    }

    @PatchMapping("/nickname")
    public ResponseEntity<MemberProfileResponse> changeNickname(
            @AuthenticationPrincipal AuthenticatedMember principal,
            @Valid @RequestBody ChangeNicknameRequest request) {
        ChangeNicknameCommand command = new ChangeNicknameCommand(
                principal.getMemberId(), request.getNickname());
        MemberProfile profile = profileService.changeNickname(command);
        return ResponseEntity.ok(MemberProfileResponse.from(profile));
    }

    @PatchMapping("/password")
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal AuthenticatedMember principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        ChangePasswordCommand command = new ChangePasswordCommand(
                principal.getMemberId(),
                request.getCurrentPassword(),
                request.getNewPassword(),
                request.getNewPasswordConfirm());
        profileService.changePassword(command);
        return ResponseEntity.ok(new MessageResponse("비밀번호가 변경되었습니다."));
    }

    @DeleteMapping
    public ResponseEntity<MessageResponse> withdraw(
            @AuthenticationPrincipal AuthenticatedMember principal,
            @Valid @RequestBody WithdrawRequest request) {
        WithdrawMemberCommand command = new WithdrawMemberCommand(
                principal.getMemberId(), request.getCurrentPassword());
        profileService.withdraw(command);
        return ResponseEntity.ok(new MessageResponse("회원 탈퇴가 완료되었습니다."));
    }
}
