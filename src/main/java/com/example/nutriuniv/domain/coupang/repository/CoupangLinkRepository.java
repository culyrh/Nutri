package com.example.nutriuniv.domain.coupang.repository;

import com.example.nutriuniv.domain.coupang.entity.CoupangLink;
import com.example.nutriuniv.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CoupangLinkRepository extends JpaRepository<CoupangLink, Long> {

    Optional<CoupangLink> findByProduct(Product product);

    Page<CoupangLink> findByLinkStatus(String linkStatus, Pageable pageable);

    List<CoupangLink> findAllByLinkStatus(String linkStatus);

    long countByLinkStatus(String linkStatus);

    // ── 수동 매핑용 ─────────────────────────────────────────────────────────────

    /**
     * 수동 매핑 작업 큐. productId ≥ fromId 이며 link_status 가 주어진 값들 중 하나인 productId 들을
     * productId 오름차순으로 반환. statuses 가 비어있으면 전체.
     */
    @Query("""
            SELECT cl.product.id
              FROM CoupangLink cl
             WHERE cl.product.id >= :fromId
               AND ( :#{#statuses.size()} = 0 OR cl.linkStatus IN :statuses )
             ORDER BY cl.product.id ASC
           """)
    List<Long> findProductIdsForManualQueue(@Param("fromId") Long fromId,
                                            @Param("statuses") List<String> statuses);
}
