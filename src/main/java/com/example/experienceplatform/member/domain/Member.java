package com.example.experienceplatform.member.domain;

import com.example.experienceplatform.member.domain.exception.AlreadyWithdrawnException;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private Email email;

    @Embedded
    private PasswordHash passwordHash;

    @Embedded
    private Nickname nickname;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

    protected Member() {
    }

    public Member(Email email, PasswordHash passwordHash, Nickname nickname) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.status = MemberStatus.ACTIVE;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void changeNickname(Nickname newNickname) {
        this.nickname = newNickname;
    }

    public void changePassword(PasswordHash newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void withdraw() {
        if (this.status == MemberStatus.WITHDRAWN) {
            throw new AlreadyWithdrawnException();
        }
        this.status = MemberStatus.WITHDRAWN;
    }

    public Long getId() {
        return id;
    }

    public Email getEmail() {
        return email;
    }

    public PasswordHash getPasswordHash() {
        return passwordHash;
    }

    public Nickname getNickname() {
        return nickname;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public MemberStatus getStatus() {
        return status;
    }
}
