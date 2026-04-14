package com.example.nutriuniv.domain.brand.repository;

import com.example.nutriuniv.domain.brand.entity.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    Optional<Brand> findByName(String name);

    Page<Brand> findByNameContaining(String keyword, Pageable pageable);
}