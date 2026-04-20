package com.example.nutriuniv.domain.coupang.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupang_daily_reports",
        uniqueConstraints = @UniqueConstraint(columnNames = {"date", "sub_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class CoupangDailyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "sub_id", length = 100)
    private String subId;

    @Column(nullable = false)
    private int click = 0;

    @Column(name = "order_count", nullable = false)
    private int orderCount = 0;

    @Column(name = "cancel_count", nullable = false)
    private int cancelCount = 0;

    @Column(nullable = false)
    private int gmv = 0;

    @Column(nullable = false)
    private int commission = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
