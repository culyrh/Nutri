package com.example.nutriuniv.domain.pns.service;

import com.example.nutriuniv.domain.pns.calculator.MealRatioResolver;
import com.example.nutriuniv.domain.pns.calculator.PnsCalculator;
import com.example.nutriuniv.domain.pns.entity.ProductPnsByEer;
import com.example.nutriuniv.domain.pns.repository.ProductPnsByEerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PnsBatchService {

    private static final int[] EER_BANDS = {1500, 2000, 2500, 3000};

    private final ProductPnsByEerRepository repository;

    /** 모든 상품에 대해 4개 EER 구간 점수/등급/백분위를 일괄 계산 후 저장. */
    @Transactional
    public BatchResult calculateAll() {
        long startMs = System.currentTimeMillis();

        // 1. 상품 + 부모 카테고리 + 영양성분 한 번에 로드
        List<Object[]> rows = repository.fetchProductsWithParentCategory();
        log.info("[PNS] {}개 상품 영양 데이터 로드", rows.size());

        if (rows.isEmpty()) {
            return new BatchResult(0, 0, EER_BANDS.length, 0L);
        }

        int totalSaved = 0;
        for (int band : EER_BANDS) {
            // 기존 구간 데이터 정리
            int deleted = repository.deleteByEerBand(band);
            log.info("[PNS] band={} 기존 {}건 삭제", band, deleted);

            // 점수 계산
            List<ProductPnsByEer> entities = new ArrayList<>(rows.size());
            // 카테고리별 점수 모음 (백분위 산출용)
            Map<Long, List<ProductPnsByEer>> byParent = new HashMap<>();

            for (Object[] r : rows) {
                Long productId       = ((Number) r[0]).longValue();
                Long parentCategory  = r[1] == null ? null : ((Number) r[1]).longValue();
                BigDecimal carb      = (BigDecimal) r[2];
                BigDecimal protein   = (BigDecimal) r[3];
                BigDecimal fat       = (BigDecimal) r[4];
                BigDecimal fiber     = (BigDecimal) r[5];
                BigDecimal cholesterol = (BigDecimal) r[6];
                BigDecimal satFat    = (BigDecimal) r[7];
                BigDecimal transFat  = (BigDecimal) r[8];
                BigDecimal sugar     = (BigDecimal) r[9];
                BigDecimal sodium    = (BigDecimal) r[10];

                double mealRatio = MealRatioResolver.resolve(parentCategory);
                PnsCalculator.Result res = PnsCalculator.calculate(
                        band, mealRatio,
                        carb, protein, fat, fiber, cholesterol,
                        satFat, transFat, sugar, sodium
                );

                ProductPnsByEer e = ProductPnsByEer.create(productId, band, res.score, res.grade);
                entities.add(e);
                byParent.computeIfAbsent(parentCategory, k -> new ArrayList<>()).add(e);
            }

            // 백분위 산출 (대분류별 점수 내림차순 → 상위 %)
            for (List<ProductPnsByEer> group : byParent.values()) {
                group.sort(Comparator.comparing(ProductPnsByEer::getScore).reversed());
                int total = group.size();
                for (int i = 0; i < total; i++) {
                    double percentile = ((double) (total - i)) / total * 100.0;
                    group.get(i).updatePercentile(percentile);
                }
            }

            repository.saveAll(entities);
            totalSaved += entities.size();
            log.info("[PNS] band={} {}개 저장 완료", band, entities.size());
        }

        long elapsed = System.currentTimeMillis() - startMs;
        log.info("[PNS] 전체 완료 — 상품 {}개 × {}개 구간 = {}건 ({}ms)",
                rows.size(), EER_BANDS.length, totalSaved, elapsed);

        return new BatchResult(rows.size(), totalSaved, EER_BANDS.length, elapsed);
    }

    public record BatchResult(int productCount, int savedRows, int bands, long elapsedMs) {}
}
