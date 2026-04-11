package com.example.nutriuniv.domain.category.repository;

import com.example.nutriuniv.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNameAndDepth(String name, int depth);

    Optional<Category> findByNameAndDepthAndParent(String name, int depth, Category parent);
}