package com.example.nutriuniv.domain.like.repository;

import com.example.nutriuniv.domain.like.entity.UserFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {

    // 찜 목록 페이징 (활성 상품만)
    Page<UserFavorite> findByUserIdAndProductIsActiveTrue(Long userId, Pageable pageable);

    // 중복 찜 체크
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    // 찜 삭제용 조회
    Optional<UserFavorite> findByUserIdAndProductId(Long userId, Long productId);

    // ProductService에서 isFavorited 체크용
    boolean existsByUserIdAndProductIdAndProductIsActiveTrue(Long userId, Long productId);

    // 추천 API에서 isFavorited 일괄 체크용
    @Query("SELECT f.product.id FROM UserFavorite f WHERE f.user.id = :userId")
    Set<Long> findFavoritedProductIdsByUserId(@Param("userId") Long userId);
}