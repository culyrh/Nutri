package com.example.nutriuniv.domain.auth.entity;

import com.example.nutriuniv.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "refresh_token", nullable = false, unique = true, length = 512)
    private String refreshToken;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static AuthToken create(User user, String refreshToken,
                                   String deviceInfo, LocalDateTime expiresAt) {
        AuthToken t = new AuthToken();
        t.user = user;
        t.refreshToken = refreshToken;
        t.deviceInfo = deviceInfo;
        t.expiresAt = expiresAt;
        return t;
    }

    public void rotate(String newRefreshToken, LocalDateTime newExpiresAt) {
        this.refreshToken = newRefreshToken;
        this.expiresAt = newExpiresAt;
    }
}