package com.example.experienceplatform.member.interfaces;

import com.example.experienceplatform.member.application.MemberInfo;
import com.example.experienceplatform.member.application.MemberService;
import com.example.experienceplatform.member.domain.Member;
import com.example.experienceplatform.member.domain.MemberRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;
    private final MemberRepository memberRepository;

    public MemberController(MemberService memberService, MemberRepository memberRepository) {
        this.memberService = memberService;
        this.memberRepository = memberRepository;
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        MemberInfo memberInfo = memberService.registerMember(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(SignupResponse.from(memberInfo));
    }

    @GetMapping("/me")
    public ResponseEntity<MemberResponse> me(@AuthenticationPrincipal AuthenticatedMember principal) {
        Member member = memberRepository.findById(principal.getMemberId())
                .orElseThrow(() -> new IllegalStateException("회원 정보를 찾을 수 없습니다."));
        return ResponseEntity.ok(MemberResponse.from(member));
    }
}
