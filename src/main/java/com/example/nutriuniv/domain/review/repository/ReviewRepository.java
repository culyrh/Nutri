package com.example.nutriuniv.domain.review.repository;

import com.example.nutriuniv.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
}
