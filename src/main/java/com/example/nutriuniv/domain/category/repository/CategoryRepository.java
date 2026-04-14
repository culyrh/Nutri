package com.example.nutriuniv.domain.category.repository;

import com.example.nutriuniv.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNameAndDepth(String name, int depth);

    Optional<Category> findByNameAndDepthAndParent(String name, int depth, Category parent);

    // 트리 조회용
    List<Category> findAllByIsActiveTrue();

    // 하위 카테고리 존재 여부 (삭제 차단)
    boolean existsByParentAndIsActiveTrue(Category parent);

    // 같은 depth + parent 내 카테고리 중복명 확인
    boolean existsByNameAndDepthAndParentAndIsActiveTrue(String name, int depth, Category parent);
}