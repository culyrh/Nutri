package com.example.nutriuniv.domain.review.entity;

import com.example.nutriuniv.domain.product.entity.Product;
import com.example.nutriuniv.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "score_taste")
    private Integer scoreTaste;

    @Column(name = "score_value")
    private Integer scoreValue;

    @Column(name = "score_overall", nullable = false)
    private int scoreOverall;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<ReviewImage> images = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Review create(User user, Product product,
                                int scoreOverall, Integer scoreTaste, Integer scoreValue,
                                String content) {
        Review r = new Review();
        r.user = user;
        r.product = product;
        r.scoreOverall = scoreOverall;
        r.scoreTaste = scoreTaste;
        r.scoreValue = scoreValue;
        r.content = content;
        return r;
    }

    public void update(int scoreOverall, Integer scoreTaste, Integer scoreValue, String content) {
        this.scoreOverall = scoreOverall;
        this.scoreTaste = scoreTaste;
        this.scoreValue = scoreValue;
        this.content = content;
    }

    public void clearImages() {
        this.images.clear();
    }

    public void deactivate() {
        this.isActive = false;
    }
}