package com.example.nutriuniv.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "oauth_provider", nullable = false, length = 20)
    private String oauthProvider;

    @Column(name = "oauth_id", nullable = false, length = 255)
    private String oauthId;

    @Column(length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false, length = 10)
    private String gender;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 10)
    private String role = "USER";

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static User create(String oauthProvider, String oauthId,
                              String name, String gender, LocalDate birthDate, String email) {
        User u = new User();
        u.oauthProvider = oauthProvider;
        u.oauthId = oauthId;
        u.name = name;
        u.nickname = name;  // 가입 시 name으로 자동 설정
        u.gender = gender;
        u.birthDate = birthDate;
        u.email = email;
        return u;
    }

    public void update(String name, String email, String nickname, String gender, LocalDate birthDate) {
        if (name != null)      this.name      = name;
        if (email != null)     this.email     = email;
        if (nickname != null)  this.nickname  = nickname;
        if (gender != null)    this.gender    = gender;
        if (birthDate != null) this.birthDate = birthDate;
    }

    public void deactivate() {
        this.isActive = false;
        this.deletedAt = LocalDateTime.now();
    }
}