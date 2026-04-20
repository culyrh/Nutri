package com.example.nutriuniv.domain.search.repository;

import com.example.nutriuniv.domain.search.entity.PopularKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PopularKeywordRepository extends JpaRepository<PopularKeyword, Long> {

    List<PopularKeyword> findTop10ByOrderByRankAsc();
}
