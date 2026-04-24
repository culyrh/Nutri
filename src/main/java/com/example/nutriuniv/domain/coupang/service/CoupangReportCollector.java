package com.example.nutriuniv.domain.coupang.service;

import com.example.nutriuniv.domain.coupang.client.CoupangApiClient;
import com.example.nutriuniv.domain.coupang.dto.*;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoupangReportCollector {

    private final CoupangApiClient coupangApiClient;
    private final EntityManager entityManager;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ── 리포트 수집 및 저장 ────────────────────────────────────────────────────────
    // 관리자가 수동으로 특정 기간의 리포트를 수집하거나,
    // 스케줄러(@Scheduled)를 통해 매일 자동 수집할 때 호출

    @Transactional
    public void collect(LocalDate start, LocalDate end) {
        String startDate = start.format(DATE_FORMATTER);
        String endDate = end.format(DATE_FORMATTER);

        log.info("[CoupangReport] 수집 시작 - {} ~ {}", startDate, endDate);

        // 4개 API 각각 호출
        Map<String, Integer> clickMap = coupangApiClient.getClickReport(startDate, endDate)
                .stream().collect(Collectors.toMap(
                        CoupangClickData::getDate,
                        CoupangClickData::getClick,
                        Integer::sum
                ));

        Map<String, int[]> orderMap = coupangApiClient.getOrderReport(startDate, endDate)
                .stream().collect(Collectors.toMap(
                        CoupangOrderData::getDate,
                        d -> new int[]{1, d.getGmv()},
                        (a, b) -> new int[]{a[0] + b[0], a[1] + b[1]}
                ));

        Map<String, Integer> cancelMap = coupangApiClient.getCancelReport(startDate, endDate)
                .stream().collect(Collectors.toMap(
                        CoupangCancelData::getDate,
                        d -> 1,
                        Integer::sum
                ));

        Map<String, Integer> commissionMap = coupangApiClient.getCommissionReport(startDate, endDate)
                .stream().collect(Collectors.toMap(
                        CoupangCommissionData::getDate,
                        CoupangCommissionData::getCommission,
                        Integer::sum
                ));

        // 날짜별 병합 후 upsert (ON CONFLICT → UPDATE)
        start.datesUntil(end.plusDays(1)).forEach(date -> {
            String dateStr = date.format(DATE_FORMATTER);
            int[] orderInfo = orderMap.getOrDefault(dateStr, new int[]{0, 0});

            entityManager.createNativeQuery("""
                    INSERT INTO coupang_daily_reports
                        (date, click, order_count, cancel_count, gmv, commission, created_at, updated_at)
                    VALUES
                        (:date, :click, :orderCount, :cancelCount, :gmv, :commission, now(), now())
                    ON CONFLICT (date, sub_id) DO UPDATE SET
                        click        = EXCLUDED.click,
                        order_count  = EXCLUDED.order_count,
                        cancel_count = EXCLUDED.cancel_count,
                        gmv          = EXCLUDED.gmv,
                        commission   = EXCLUDED.commission,
                        updated_at   = now()
                    """)
                    .setParameter("date", date)
                    .setParameter("click", clickMap.getOrDefault(dateStr, 0))
                    .setParameter("orderCount", orderInfo[0])
                    .setParameter("cancelCount", cancelMap.getOrDefault(dateStr, 0))
                    .setParameter("gmv", orderInfo[1])
                    .setParameter("commission", commissionMap.getOrDefault(dateStr, 0))
                    .executeUpdate();
        });

        log.info("[CoupangReport] 수집 완료 - {} ~ {}", startDate, endDate);
    }

    // 어제 하루치 자동 수집 (스케줄러용)
    @Transactional
    public void collectYesterday() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        collect(yesterday, yesterday);
    }
}
