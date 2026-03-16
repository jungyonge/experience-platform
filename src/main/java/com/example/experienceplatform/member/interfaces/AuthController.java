package com.example.experienceplatform.member.interfaces;

import com.example.experienceplatform.member.application.AuthService;
import com.example.experienceplatform.member.application.AuthTokenInfo;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthTokenInfo tokenInfo = authService.login(request.toCommand());
        return ResponseEntity.ok(LoginResponse.from(tokenInfo));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthTokenInfo tokenInfo = authService.refresh(request.toCommand());
        return ResponseEntity.ok(LoginResponse.from(tokenInfo));
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@AuthenticationPrincipal AuthenticatedMember principal) {
        authService.logout(principal.getMemberId());
        return ResponseEntity.ok(new LogoutResponse("로그아웃 되었습니다."));
    }
}
