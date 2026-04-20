package com.example.nutriuniv.domain.coupang.repository;

import com.example.nutriuniv.domain.coupang.entity.CoupangDailyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CoupangDailyReportRepository extends JpaRepository<CoupangDailyReport, Long> {

    List<CoupangDailyReport> findByDateBetweenOrderByDateAsc(LocalDate start, LocalDate end);
}
